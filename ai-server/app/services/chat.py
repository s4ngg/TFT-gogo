"""
AI 채팅 서비스

시스템 프롬프트 초안(tft_ai_system_prompt_draft.md)을 기반으로 설계.
사용자 질문에 대해 실데이터/일반지식 구분, 명칭 매핑 원칙 등 5가지 절대 원칙을 준수한다.
"""
import logging
import re

from openai import AsyncOpenAI, APIStatusError, APITimeoutError, APIConnectionError

from app.core.config import settings
from app.models.chat import ChatContext, ChatMessage, ChatRequest

logger = logging.getLogger(__name__)

_client: AsyncOpenAI | None = None

_SYSTEM_PROMPT = """\
당신은 TFT(전략적 팀 전투) 전적검색 사이트의 AI 어시스턴트입니다.
Riot API를 통해 가져온 실제 데이터를 바탕으로 사용자의 전적, 유닛/증강체 통계, 메타 정보를 분석해서 알려주는 역할입니다.
가장 중요한 책임은 **정확성과 투명성**입니다. 데이터가 없으면 없다고 말하는 답이 항상 우선입니다.

## 절대 원칙

### 원칙 1: 명칭 매핑
- 유닛명, 증강체명, 특성(시너지)명은 사용자가 제공한 이름 그대로 사용합니다.
- 확실하지 않으면 사용자에게 후보를 제시하고 확인을 요청합니다.
  예: "말씀하신 '크아쉬'가 혹시 카서스를 말씀하시는 건가요? 정확한 이름을 알려주세요."
- fuzzy match를 사용했다면 응답에 반드시 명시합니다. "~로 이해하고 분석했어요"처럼.

### 원칙 2: 실데이터 vs 일반 지식 구분
- 제공된 실제 수치(전적 통계)와 일반적인 TFT 지식은 반드시 별도 블록으로 구분합니다.
- 실데이터가 없으면 답변 상단에 표시합니다:
  ⚠️ 이 항목에 대한 실시간 통계 데이터가 없어, 일반적으로 알려진 운영 방식을 안내해 드려요.

### 원칙 3: 개인 전적 vs 전체 메타 통계 구분
- 개인 전적과 전체 메타 통계를 한 문장에 섞지 않습니다.
- **[내 전적]** 또는 **[전체 평균/메타 통계]** 라벨을 붙여 출처를 명시합니다.

### 원칙 4: 없는 데이터는 추측하지 않는다
- 제공된 정보에 없는 필드는 절대 만들어내지 않습니다. "확인 불가"로 답합니다.

### 원칙 5: 의미 없는 고정 문구 금지
- 맥락과 무관하게 반복되는 필러 문장을 사용하지 않습니다.
- 정보가 부족하면 정확히 어떤 정보가 부족한지만 말합니다.

## 응답 포맷
- 실데이터 기반 통계: 출처 라벨 포함
- 확인 안 된 정보: "확인 불가" 또는 "데이터 없음"으로 명시, 추측 표현(~일 거예요, ~인 것 같아요) 금지

## TFT 특화 규칙
- 지표는 TFT 고유 지표(평균 등수, TOP4율, 1등률) 기준으로 답합니다.
- 모든 Set 번호를 응답에 명시합니다(제공된 경우).

## 톤
- 친근하고 간결한 톤, 이모지는 가독성을 위해 절제해서 사용
- 한국어로만 답합니다.
"""

_FALLBACK_REPLY = "죄송합니다. 현재 AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."

# 채팅 1회 최대 토큰 (응답 길이 제한)
_MAX_TOKENS = 600


def _get_client() -> AsyncOpenAI:
    global _client
    if _client is None:
        _client = AsyncOpenAI(api_key=settings.openai_api_key)
    return _client


def _sanitize(value: str) -> str:
    """프롬프트 삽입 방지용 정제 — 개행·따옴표 계열 제거."""
    return re.sub(r'[\n\r"\'\`]', " ", value).strip()


def _build_context_block(context: ChatContext) -> str:
    """소환사 컨텍스트를 시스템 프롬프트에 주입할 블록으로 변환."""
    lines = ["## 현재 사용자 컨텍스트 (제공된 실데이터)"]

    if context.summoner_name and context.tag_line:
        name = _sanitize(context.summoner_name)
        tag = _sanitize(context.tag_line)
        lines.append(f"- 소환사: {name}#{tag}")

    if context.stats_summary:
        lines.append(f"- 최근 전적 요약: {_sanitize(context.stats_summary)}")

    if context.good_traits:
        traits = ", ".join(_sanitize(t) for t in context.good_traits[:5])
        lines.append(f"- 잘 맞는 시너지: {traits}")

    if context.bad_traits:
        traits = ", ".join(_sanitize(t) for t in context.bad_traits[:5])
        lines.append(f"- 잘 안 맞는 시너지: {traits}")

    lines.append("이 데이터는 실제 Riot API 기반 데이터입니다. 위 수치를 인용할 때 [내 전적] 라벨을 붙이세요.")
    return "\n".join(lines)


def _build_messages(request: ChatRequest) -> list[dict]:
    system_content = _SYSTEM_PROMPT

    if request.context:
        context_block = _build_context_block(request.context)
        system_content = f"{_SYSTEM_PROMPT}\n\n{context_block}"

    messages: list[dict] = [{"role": "system", "content": system_content}]
    for msg in request.messages:
        messages.append({"role": msg.role, "content": msg.content})
    return messages


async def chat(request: ChatRequest) -> str:
    """사용자 메시지에 대한 AI 응답을 생성한다. 오류 시 fallback 문자열 반환."""
    if not settings.openai_api_key:
        logger.warning("OpenAI API 키 미설정 — fallback 응답 반환")
        return _FALLBACK_REPLY

    messages = _build_messages(request)

    try:
        client = _get_client()
        response = await client.chat.completions.create(
            model=settings.openai_model,
            messages=messages,
            temperature=0.4,
            max_tokens=_MAX_TOKENS,
        )
        return response.choices[0].message.content or _FALLBACK_REPLY
    except APITimeoutError as e:
        logger.warning("OpenAI 채팅 타임아웃, fallback 사용: %s", e)
        return _FALLBACK_REPLY
    except APIConnectionError as e:
        logger.warning("OpenAI 채팅 연결 실패, fallback 사용: %s", e)
        return _FALLBACK_REPLY
    except APIStatusError as e:
        logger.warning("OpenAI 채팅 API 오류 status=%s, fallback 사용: %s", e.status_code, e)
        return _FALLBACK_REPLY
    except Exception as e:
        logger.warning("OpenAI 채팅 예상치 못한 오류, fallback 사용: %s", e)
        return _FALLBACK_REPLY
