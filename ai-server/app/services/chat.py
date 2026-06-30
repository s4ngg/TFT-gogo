"""
AI 채팅 서비스

시스템 프롬프트 초안(tft_ai_system_prompt_draft.md)을 기반으로 설계.
사용자 질문에 대해 실데이터/일반지식 구분, 명칭 매핑 원칙 등 5가지 절대 원칙을 준수한다.
"""
import logging
import re

from openai import AsyncOpenAI, APIStatusError, APITimeoutError, APIConnectionError

from app.core.ai_logger import AiRequestLog
from app.core.circuit_breaker import openai_breaker
from app.core.config import settings
from app.core.token_budget import check_budget
from app.models.chat import ChatContext, ChatRequest

logger = logging.getLogger(__name__)

_client: AsyncOpenAI | None = None

_SYSTEM_PROMPT = """\
당신은 TFT(전략적 팀 전투) 전적검색 사이트의 AI 어시스턴트입니다.
사용자의 TFT 관련 질문에 친절하고 정확하게 답변하는 것이 주 역할입니다.
덱 추천, 시너지 설명, 아이템 조합, 챔피언 분석, 메타 분석, 운영 팁 등 일반 TFT 지식 질문에는 자유롭게 답변합니다.
사용자 컨텍스트(전적 데이터)가 제공된 경우, 해당 데이터를 활용해 개인화된 분석도 제공합니다.

## 절대 원칙

### 원칙 1: 명칭 매핑
- 유닛명, 증강체명, 특성(시너지)명은 사용자가 제공한 이름 그대로 사용합니다.
- 확실하지 않으면 사용자에게 후보를 제시하고 확인을 요청합니다.

### 원칙 2: 실데이터 vs 일반 지식 구분
- 제공된 실제 수치(전적 통계)를 인용할 때는 **[내 전적]** 라벨을 붙입니다.
- 일반 TFT 지식으로 답변할 때는 라벨 없이 바로 답변합니다.
- 사용자가 개인 전적을 물었는데 컨텍스트에 해당 데이터가 없을 때만 "전적 데이터가 없어 확인할 수 없어요"라고 안내합니다.

### 원칙 3: 없는 데이터는 추측하지 않는다
- 제공된 전적 정보에 없는 수치를 만들어내지 않습니다.
- 단, 일반 TFT 지식(덱 구성, 아이템 조합 등)은 자유롭게 답변합니다.

### 원칙 4: 의미 없는 고정 문구 금지
- 맥락과 무관하게 반복되는 경고 문구나 필러 문장을 사용하지 않습니다.
- 질문에 바로 답합니다.

## TFT 특화 규칙
- 지표는 TFT 고유 지표(평균 등수, TOP4율, 1등률) 기준으로 답합니다.

## 제공 데이터의 한계
사용자 컨텍스트에는 다음 데이터만 포함됩니다:
- 최근 20게임의 순위, 게임 타입, 덱 이름, 주요 챔피언(상위 4개)
- 많이 사용한 챔피언 통계(게임 수, 평균 등수)
- 많이 사용한 시너지 통계(게임 수, 평균 등수)
- 전체 요약(평균 등수, TOP4율, 1등률)

다음 데이터는 포함되지 않으므로 질문 시 솔직히 안내합니다:
- 아이템 빌드 정보 (어떤 아이템을 조합했는지)
- 증강체 선택 정보
- LP 변동 및 승급/강등 이력
- 다른 소환사의 전적
- 전체 서버 메타 통계

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
        _client = AsyncOpenAI(
            api_key=settings.openai_api_key,
            timeout=settings.openai_timeout,
        )
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

    if context.top_champions:
        champs = ", ".join(_sanitize(c) for c in context.top_champions[:10])
        lines.append(f"- 많이 사용한 챔피언: {champs}")

    if context.recent_matches:
        lines.append(f"\n## 최근 개별 매치 기록\n{_sanitize(context.recent_matches)}")

    lines.append("\n이 데이터는 실제 Riot API 기반 데이터입니다. 위 수치를 인용할 때 [내 전적] 라벨을 붙이세요.")
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
    log = AiRequestLog(feature="chat", model=settings.openai_model)

    if not settings.openai_api_key:
        logger.warning("OpenAI API 키 미설정 — fallback 응답 반환")
        log.is_fallback = True
        log.emit()
        return _FALLBACK_REPLY

    messages = _build_messages(request)

    estimated = check_budget(messages, settings.chat_max_input_tokens, "chat")
    log.input_tokens_estimated = estimated

    if openai_breaker.is_open():
        logger.warning("[chat] Circuit breaker OPEN — fallback 반환")
        log.is_fallback = True
        log.emit()
        return _FALLBACK_REPLY

    log.start_timer()
    try:
        client = _get_client()
        response = await client.chat.completions.create(
            model=settings.openai_model,
            messages=messages,
            temperature=0.4,
            max_tokens=_MAX_TOKENS,
        )
        log.stop_timer()
        if response.usage:
            log.output_tokens = response.usage.completion_tokens

        reply = response.choices[0].message.content or ""
        if not reply.strip():
            log.is_fallback = True
            reply = _FALLBACK_REPLY

        openai_breaker.record_success()
        log.emit()
        return reply
    except (APITimeoutError, APIConnectionError, APIStatusError) as e:
        log.stop_timer()
        log.is_fallback = True
        openai_breaker.record_failure()
        log.emit()
        logger.warning("OpenAI 채팅 오류, fallback 사용: %s", e)
        return _FALLBACK_REPLY
    except Exception as e:
        log.stop_timer()
        log.is_fallback = True
        openai_breaker.record_failure()
        log.emit()
        logger.warning("OpenAI 채팅 예상치 못한 오류, fallback 사용: %s", e)
        return _FALLBACK_REPLY
