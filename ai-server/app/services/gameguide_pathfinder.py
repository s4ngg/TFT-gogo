"""
GameGuide AI Pathfinder 서비스.

Guide 정적 데이터와 사용자의 질문을 바탕으로 학습/운영 루트를 구조화한다.
"""
import json
import logging
import re

from openai import APIConnectionError, APIStatusError, APITimeoutError, AsyncOpenAI
from pydantic import ValidationError

from app.core.ai_logger import AiRequestLog
from app.core.circuit_breaker import openai_breaker
from app.core.config import settings
from app.core.token_budget import check_budget
from app.models.gameguide_pathfinder import (
    CandidateGuideRef,
    GameGuidePathfinderRequest,
    GameGuidePathfinderResponse,
    GuideRef,
    PhasePlan,
)

logger = logging.getLogger(__name__)

_client: AsyncOpenAI | None = None
_MAX_TOKENS = 1200
_MAX_SUMMARY_LENGTH = 600
_MAX_DATA_STRING_LENGTH = 240
_MAX_DATA_LIST_ITEMS = 8
_MAX_DATA_DICT_ITEMS = 16
_MAX_DATA_DEPTH = 4
_MAX_RESPONSE_REFS = 5

_SYSTEM_PROMPT = """\
당신은 TFT 게임가이드 페이지의 GameGuide AI입니다.
당신의 역할은 초보자와 중급자 사이의 사용자를 돕는 실전형 TFT 코치입니다.

사용자는 TFT 개념을 완전히 모를 수도 있고, 어느 정도 알지만
"지금 무엇을 잡고, 무엇으로 이어가고, 언제 바꿔야 하는지"를 어려워할 수 있습니다.
답변은 쉬운 말로 시작하되, 실제 게임 안에서 바로 써먹을 수 있는 운영 판단까지 포함합니다.

## 답변 스타일
- 한국어로 답합니다.
- 먼저 1~2문장으로 결론을 말합니다.
- 핵심 이유는 2~3개로 정리합니다.
- 가능하면 초반, 중반, 후반으로 나누어 행동을 제안합니다.
- 챗봇에서 읽기 좋게 짧고 밀도 있게 답합니다.
- 초보자에게는 용어를 쉽게 풀고, 중급자에게는 전환/아이템/시너지 판단을 제공합니다.
- 말투는 친절하지만 과하게 가볍지 않게, 게임 코치처럼 말합니다.

## 데이터 사용 원칙
- selected_entries는 사용자가 현재 집중하고 있는 핵심 가이드입니다.
- candidate_refs는 답변에서 추천하거나 연결할 수 있는 후보입니다.
- 제공된 selected_entries와 candidate_refs를 우선 근거로 사용합니다.
- 제공된 guide ref에 없는 항목을 recommended_refs, source_refs, phase_plan.guide_refs에 넣지 않습니다.
- 현재 Guide 데이터에 없는 승률, 평균 등수, 픽률, TOP4율, 티어 판단은 생성하거나 추측하지 않습니다.
- 수치가 없으면 수치처럼 말하지 말고, "현재 제공된 가이드 데이터만으로는 확정하기 어렵다"고 말합니다.

## 이어묻기 처리
- conversation_history는 사용자가 이전 답변에 이어서 질문할 때의 문맥입니다.
- conversation_history는 질문 의도를 파악하는 데만 사용하고, 새로운 가이드 근거처럼 취급하지 않습니다.
- 이전 답변을 참조할 때는 "앞에서 말한 운영 흐름 기준으로"처럼 자연스럽게 이어서 답합니다.
- 현재 question이 짧거나 대명사 중심이면 conversation_history를 활용해 무엇을 이어 묻는지 해석합니다.
- conversation_history와 현재 Guide 데이터가 충돌하면 현재 Guide 데이터를 우선합니다.

## 가이드 근거와 AI 제안 구분
- evidence_notes에는 반드시 제공된 가이드 데이터에서 확인 가능한 내용만 씁니다.
- creative_suggestions에는 일반 TFT 운영 감각에 기반한 가능한 선택지를 씁니다.
- creative_suggestions는 확정 표현을 피하고 "가능한 방향", "시도해볼 만한 선택지", "상황에 따라"처럼 표현합니다.
- 창의 제안이 포함되면 limitations에 그 제안이 확정 데이터가 아니라는 한계를 짧게 남깁니다.
- 가이드 데이터와 AI 제안이 충돌하면 가이드 데이터를 우선합니다.

## 질문 의도 처리
사용자 질문을 내부적으로 다음 중 하나로 판단하고 답합니다.
- 개념 설명: 쉬운 정의 + 왜 중요한지 + 관련 가이드 항목
- 운영 질문: 초반/중반/후반 행동 계획
- 아이템 질문: 누구에게 어울리는지 + 왜 어울리는지 + 주의점
- 시너지 질문: 핵심 유닛/확장 방향/무리하면 안 되는 상황
- 증강체 질문: 어떤 판에서 고르면 좋은지 + 피해야 할 상황
- 전환 질문: 지금 방향을 유지할지, 다른 방향으로 틀지 판단 기준
- 모호한 질문: 가능한 해석을 먼저 제시하고, 마지막에 확인 질문을 짧게 덧붙임

## 응답 형식
응답은 반드시 JSON 객체만 출력합니다.
마크다운, 코드블록, JSON 밖 설명 문장은 쓰지 않습니다.

{
  "title": "string",
  "summary": "string",
  "core_concepts": ["string"],
  "evidence_notes": ["string"],
  "creative_suggestions": ["string"],
  "phase_plan": [
    {
      "phase": "EARLY|MID|LATE|ANY",
      "title": "string",
      "description": "string",
      "guide_refs": [
        {"guide_type": "TRAIT|ITEM|AUGMENT|CHAMPION", "target_key": "string", "name": "string"}
      ]
    }
  ],
  "recommended_refs": [
    {"guide_type": "TRAIT|ITEM|AUGMENT|CHAMPION", "target_key": "string", "name": "string", "reason": "string"}
  ],
  "avoid_mistakes": ["string"],
  "source_refs": [
    {"guide_type": "TRAIT|ITEM|AUGMENT|CHAMPION", "target_key": "string", "name": "string"}
  ],
  "limitations": ["string"],
  "is_fallback": false
}
"""


def _get_client() -> AsyncOpenAI:
    global _client
    if _client is None:
        _client = AsyncOpenAI(
            api_key=settings.openai_api_key,
            timeout=settings.openai_timeout,
        )
    return _client


def _sanitize(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r'[\n\r"\'`]', " ", value).strip()


def _truncate(value: str, max_length: int) -> str:
    if len(value) <= max_length:
        return value
    return f"{value[:max_length].rstrip()}..."


def _compact_data_value(value: object, depth: int = 0) -> object:
    if depth >= _MAX_DATA_DEPTH:
        return _truncate(str(value), _MAX_DATA_STRING_LENGTH)
    if isinstance(value, str):
        return _truncate(value, _MAX_DATA_STRING_LENGTH)
    if isinstance(value, list):
        return [
            _compact_data_value(item, depth + 1)
            for item in value[:_MAX_DATA_LIST_ITEMS]
        ]
    if isinstance(value, dict):
        return {
            str(key): _compact_data_value(item, depth + 1)
            for key, item in list(value.items())[:_MAX_DATA_DICT_ITEMS]
        }
    return value


def _tab_label(active_tab: str) -> str:
    labels = {
        "traits": "시너지",
        "items": "아이템",
        "augments": "증강체",
        "champions": "챔피언",
    }
    return labels.get(active_tab, "게임가이드")


def _source_refs(request: GameGuidePathfinderRequest) -> list[GuideRef]:
    return [
        GuideRef(
            guide_type=entry.guide_type,
            target_key=entry.target_key,
            name=entry.name,
        )
        for entry in request.selected_entries
    ]


def _fallback_response(request: GameGuidePathfinderRequest) -> GameGuidePathfinderResponse:
    tab_label = _tab_label(request.active_tab)

    return GameGuidePathfinderResponse(
        title=f"{tab_label} 가이드 질문",
        summary="GameGuide AI 연결 전이라 현재 가이드 화면 기준의 기본 안내를 표시합니다.",
        core_concepts=[
            "질문 키워드를 현재 가이드 탭에서 먼저 검색해 관련 항목을 확인하세요.",
            "시너지, 챔피언, 아이템, 증강체를 하나씩 연결해 운영 흐름을 좁히는 방식이 안전합니다.",
        ],
        evidence_notes=[
            "현재 선택한 가이드 항목과 화면 후보만 기준으로 안내합니다.",
        ],
        creative_suggestions=[],
        phase_plan=[
            PhasePlan(
                phase="ANY",
                title="가이드 항목 확인",
                description="현재 탭에서 질문 키워드와 가장 가까운 가이드 항목을 먼저 확인합니다.",
                guide_refs=[],
            ),
            PhasePlan(
                phase="ANY",
                title="연관 정보 비교",
                description="연관된 시너지, 챔피언, 아이템을 순서대로 비교하며 다음 질문을 좁힙니다.",
                guide_refs=[],
            ),
        ],
        recommended_refs=[],
        avoid_mistakes=[
            "현재 단계에서는 실제 메타 수치나 승률을 판단하지 않습니다.",
            "가이드 데이터에 없는 아이템, 증강체, 전적 정보는 단정하지 않습니다.",
        ],
        source_refs=_source_refs(request),
        limitations=[
            "아직 GameGuide AI 응답을 생성할 수 없어 기본 안내를 표시합니다.",
            f"질문: {_sanitize(request.question)}",
        ],
        is_fallback=True,
    )


def _compact_entry(entry: object) -> dict:
    if hasattr(entry, "model_dump"):
        raw = entry.model_dump()
    else:
        raw = {}
    summary = raw.get("summary")
    reason_hint = raw.get("reason_hint")
    return {
        "guide_type": raw.get("guide_type"),
        "target_key": raw.get("target_key"),
        "name": raw.get("name"),
        "summary": _truncate(_sanitize(summary), _MAX_SUMMARY_LENGTH) if summary else None,
        "data": _compact_data_value(raw.get("data", {})),
        "reason_hint": _truncate(_sanitize(reason_hint), 200) if reason_hint else None,
    }


def _compact_conversation_message(message: object) -> dict:
    if hasattr(message, "model_dump"):
        raw = message.model_dump()
    else:
        raw = {}
    return {
        "role": raw.get("role"),
        "content": _truncate(_sanitize(raw.get("content")), 700),
    }


def _build_user_payload(request: GameGuidePathfinderRequest) -> str:
    payload = {
        "patch_version": request.patch_version,
        "active_tab": request.active_tab,
        "mode": request.mode,
        "question": _sanitize(request.question),
        "conversation_history": [
            _compact_conversation_message(message)
            for message in request.conversation_history
        ],
        "selected_entries": [_compact_entry(entry) for entry in request.selected_entries],
        "candidate_refs": [_compact_entry(ref) for ref in request.candidate_refs],
    }
    return json.dumps(payload, ensure_ascii=False)


def _build_messages(request: GameGuidePathfinderRequest) -> list[dict]:
    return [
        {"role": "system", "content": _SYSTEM_PROMPT},
        {"role": "user", "content": _build_user_payload(request)},
    ]


def _strip_json_fence(content: str) -> str:
    return re.sub(r"```json\s*|\s*```", "", content).strip()


def _allowed_ref_keys(request: GameGuidePathfinderRequest) -> set[tuple[str, str]]:
    refs: list[GuideRef | CandidateGuideRef] = [*request.selected_entries, *request.candidate_refs]
    return {(ref.guide_type, ref.target_key) for ref in refs}


def _filter_refs(
    response: GameGuidePathfinderResponse,
    request: GameGuidePathfinderRequest,
) -> GameGuidePathfinderResponse:
    allowed = _allowed_ref_keys(request)

    def guide_ref_allowed(ref: GuideRef) -> bool:
        return (ref.guide_type, ref.target_key) in allowed

    filtered_phase_plan = [
        PhasePlan(
            phase=phase.phase,
            title=phase.title,
            description=phase.description,
            guide_refs=[
                ref for ref in phase.guide_refs
                if guide_ref_allowed(ref)
            ][:_MAX_RESPONSE_REFS],
        )
        for phase in response.phase_plan
    ]
    filtered_recommended = [
        ref for ref in response.recommended_refs
        if (ref.guide_type, ref.target_key) in allowed
    ][:_MAX_RESPONSE_REFS]
    filtered_source = [
        ref for ref in response.source_refs
        if guide_ref_allowed(ref)
    ][:_MAX_RESPONSE_REFS] or _source_refs(request)[:_MAX_RESPONSE_REFS]

    return response.model_copy(update={
        "phase_plan": filtered_phase_plan,
        "recommended_refs": filtered_recommended,
        "source_refs": filtered_source,
        "is_fallback": False,
    })


async def pathfind(request: GameGuidePathfinderRequest) -> GameGuidePathfinderResponse:
    """GameGuide 질문에 대한 구조화 응답을 생성한다. 오류 시 fallback을 반환한다."""
    if not settings.openai_api_key:
        return _fallback_response(request)

    messages = _build_messages(request)
    log = AiRequestLog(feature="gameguide-pathfinder", model=settings.openai_model)
    estimated = check_budget(messages, settings.recommend_max_input_tokens, "gameguide-pathfinder")
    log.input_tokens_estimated = estimated

    if openai_breaker.is_open():
        logger.warning("[gameguide-pathfinder] Circuit breaker OPEN — fallback 반환")
        log.is_fallback = True
        log.emit()
        return _fallback_response(request)

    log.start_timer()
    try:
        client = _get_client()
        response = await client.chat.completions.create(
            model=settings.openai_model,
            messages=messages,
            temperature=0.25,
            max_tokens=_MAX_TOKENS,
            response_format={"type": "json_object"},
        )
        log.stop_timer()
        if response.usage:
            log.output_tokens = response.usage.completion_tokens

        content = _strip_json_fence(response.choices[0].message.content or "")
        parsed = GameGuidePathfinderResponse.model_validate(json.loads(content))
        openai_breaker.record_success()
        log.emit()
        return _filter_refs(parsed, request)
    except (json.JSONDecodeError, ValidationError) as e:
        log.stop_timer()
        log.is_fallback = True
        openai_breaker.record_failure()
        log.emit()
        logger.warning("GameGuide AI 응답 파싱 실패, fallback 사용: %s", e)
        return _fallback_response(request)
    except (APITimeoutError, APIConnectionError, APIStatusError) as e:
        log.stop_timer()
        log.is_fallback = True
        openai_breaker.record_failure()
        log.emit()
        logger.warning("GameGuide AI OpenAI 오류, fallback 사용: %s", e)
        return _fallback_response(request)
    except Exception as e:
        log.stop_timer()
        log.is_fallback = True
        openai_breaker.record_failure()
        log.emit()
        logger.warning("GameGuide AI 예상치 못한 오류, fallback 사용: %s", e)
        return _fallback_response(request)
