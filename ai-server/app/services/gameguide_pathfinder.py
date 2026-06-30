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

_SYSTEM_PROMPT = """\
당신은 TFT 게임가이드 페이지의 GameGuide AI입니다.
사용자의 질문과 제공된 Guide 정적 데이터만 사용해 한국어로 답합니다.

## 절대 원칙
- 응답은 JSON 객체만 출력합니다. 마크다운, 코드블록, 설명 문장을 JSON 밖에 쓰지 않습니다.
- 제공된 selected_entries와 candidate_refs에 없는 guide ref를 링크 추천으로 만들지 않습니다.
- 현재 Guide 데이터에 없는 승률, 평균 등수, 픽률, TOP4율, 티어 판단을 생성하거나 추측하지 않습니다.
- 소환사 전적, 개인 플레이 스타일, 메타 덱 순위는 이 기능의 데이터가 아니므로 말하지 않습니다.
- 질문 의도를 자동으로 판단해 개념 설명, 운영 흐름, 시너지 확장, 아이템 활용, 전환 후보를 통합해서 답합니다.

## JSON 스키마
{
  "title": "string",
  "summary": "string",
  "core_concepts": ["string"],
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


def _build_user_payload(request: GameGuidePathfinderRequest) -> str:
    payload = {
        "patch_version": request.patch_version,
        "active_tab": request.active_tab,
        "mode": request.mode,
        "question": _sanitize(request.question),
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
            guide_refs=[ref for ref in phase.guide_refs if guide_ref_allowed(ref)],
        )
        for phase in response.phase_plan
    ]
    filtered_recommended = [
        ref for ref in response.recommended_refs
        if (ref.guide_type, ref.target_key) in allowed
    ]
    filtered_source = [
        ref for ref in response.source_refs
        if guide_ref_allowed(ref)
    ] or _source_refs(request)

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
