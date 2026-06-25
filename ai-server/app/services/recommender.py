"""
메타 매칭 + OpenAI 추천 이유 생성 서비스

내 플레이 스타일(시너지 통계)과 현재 메타 덱을 매칭해
추천 이유를 OpenAI로 생성한다.
"""
import json
import logging
import re

from openai import AsyncOpenAI, APIStatusError, APITimeoutError, APIConnectionError

from app.core.ai_logger import AiRequestLog
from app.core.circuit_breaker import openai_breaker
from app.core.config import settings
from app.core.token_budget import check_budget
from app.models.match import DeckReason, MetaDeck, TraitStat

logger = logging.getLogger(__name__)

_client: AsyncOpenAI | None = None

# 추천 이유 1건당 예상 토큰 (한국어 1~2문장 + JSON 키)
_TOKENS_PER_DECK = 80
_TOKENS_BASE = 50  # JSON 배열 오버헤드

# S/A 티어 덱 = 패치 후 상위 메타 등극 기준으로 isPatchTrend 판단
_PATCH_TREND_GRADES = {"S", "A"}


def _get_client() -> AsyncOpenAI:
    global _client
    if _client is None:
        _client = AsyncOpenAI(
            api_key=settings.openai_api_key,
            timeout=settings.openai_timeout,
        )
    return _client


def _sanitize(value: str) -> str:
    """프롬프트 삽입 방지용 최소 정제 — 개행·따옴표 계열 제거."""
    return re.sub(r'[\n\r"\'\`]', " ", value).strip()


def _match_score(deck: MetaDeck, good_traits: list[TraitStat]) -> float:
    """
    메타 덱과 내 잘 맞는 시너지의 겹침 점수 계산.
    겹치는 시너지가 많고 내 평균 등수가 낮을수록 높은 점수.
    """
    good_names = {t.name.lower() for t in good_traits}
    deck_traits = {s.lower() for s in deck.trait_suffixes}
    overlap = good_names & deck_traits

    if not overlap:
        return 0.0

    # 겹치는 시너지의 평균 등수 평균 (낮을수록 좋음 → 점수 높음)
    overlap_stats = [t for t in good_traits if t.name.lower() in overlap]
    avg_place = sum(float(t.avg_place) for t in overlap_stats) / len(overlap_stats)

    return len(overlap) * (8 - avg_place)


def rank_meta_decks(
    meta_decks: list[MetaDeck],
    good_traits: list[TraitStat],
) -> list[MetaDeck]:
    """내 플레이 스타일 기반으로 메타 덱 정렬."""
    scored = [(deck, _match_score(deck, good_traits)) for deck in meta_decks]
    scored.sort(key=lambda x: x[1], reverse=True)
    return [deck for deck, _ in scored]


async def generate_reasons(
    good_traits: list[TraitStat],
    bad_traits: list[TraitStat],
    recommended_decks: list[MetaDeck],
    stats_summary: str,
) -> list[DeckReason]:
    """
    OpenAI API로 추천 이유 생성.
    API 키 미설정 시 기본 메시지로 fallback.
    """
    if not settings.openai_api_key:
        return _fallback_reasons(recommended_decks)

    target_decks = recommended_decks[:3]

    # 겹치는 시너지 기반으로 덱별 추천 근거를 미리 계산 (프롬프트 품질 향상)
    good_names = {t.name.lower() for t in good_traits}
    bad_names = {t.name.lower() for t in bad_traits}

    deck_info = []
    for d in target_decks:
        deck_traits_lower = {s.lower() for s in d.trait_suffixes[:4]}
        matched = sorted(_sanitize(n) for n in (good_names & deck_traits_lower))
        avoid = sorted(_sanitize(n) for n in (bad_names & deck_traits_lower))
        deck_info.append({
            "rank": d.rank,
            "grade": _sanitize(d.grade),
            "traits": [_sanitize(s) for s in d.trait_suffixes[:4]],
            "matched_with_player": matched,
            "player_weak_traits": avoid,
            "avg_place": _sanitize(d.avg_place),
            "top4_rate": _sanitize(d.top4_rate),
        })

    good_trait_summary = ", ".join(
        f"{_sanitize(t.name)}(평균{_sanitize(t.avg_place)}등, TOP4 {_sanitize(t.top4_rate)})"
        for t in good_traits[:3]
    )
    bad_trait_summary = ", ".join(_sanitize(t.name) for t in bad_traits[:3])

    system_message = (
        "당신은 TFT(팀파이트 택티스) 전문 코치입니다. "
        "플레이어 통계와 덱 데이터를 바탕으로 추천 이유를 한국어 1~2문장으로 작성합니다. "
        "잘 맞는 시너지가 겹치면 구체적으로 언급하고, 주의할 약점도 간략히 포함하세요. "
        "반드시 아래 JSON 배열 형식만 출력하세요. 다른 텍스트는 절대 출력하지 마세요:\n"
        '[{"deck_rank": <숫자>, "reason": "<추천 이유>"}]'
    )
    user_message = (
        f"플레이어 최근 통계: {_sanitize(stats_summary)}\n"
        f"잘 맞는 시너지: {good_trait_summary}\n"
        f"약한 시너지: {bad_trait_summary}\n\n"
        f"추천 덱 목록:\n{json.dumps(deck_info, ensure_ascii=False)}"
    )

    messages = [
        {"role": "system", "content": system_message},
        {"role": "user", "content": user_message},
    ]

    log = AiRequestLog(feature="recommend", model=settings.openai_model)

    estimated = check_budget(messages, settings.recommend_max_input_tokens, "recommend")
    log.input_tokens_estimated = estimated

    if openai_breaker.is_open():
        logger.warning("[recommend] Circuit breaker OPEN — fallback 반환")
        log.is_fallback = True
        log.emit()
        return _fallback_reasons(recommended_decks)

    # 추천 덱 수에 비례해 max_tokens 동적 계산
    max_tokens = _TOKENS_BASE + len(target_decks) * _TOKENS_PER_DECK

    log.start_timer()
    try:
        client = _get_client()
        response = await client.chat.completions.create(
            model=settings.openai_model,
            messages=messages,
            temperature=0.3,
            max_tokens=max_tokens,
        )
        log.stop_timer()
        if response.usage:
            log.output_tokens = response.usage.completion_tokens
        openai_breaker.record_success()
        log.emit()

        content = response.choices[0].message.content or ""
        content = re.sub(r"```json\s*|\s*```", "", content).strip()
        raw = json.loads(content)
        return [
            DeckReason(
                deck_rank=item["deck_rank"],
                is_patch_trend=_is_patch_trend(item["deck_rank"], target_decks),
                reason=item["reason"],
            )
            for item in raw
        ]
    except json.JSONDecodeError as e:
        logger.warning("OpenAI 응답 JSON 파싱 실패, fallback 사용: %s", e)
        return _fallback_reasons(recommended_decks)
    except (APITimeoutError, APIConnectionError, APIStatusError) as e:
        log.stop_timer()
        log.is_fallback = True
        openai_breaker.record_failure()
        log.emit()
        logger.warning("OpenAI 추천 API 오류, fallback 사용: %s", e)
        return _fallback_reasons(recommended_decks)
    except Exception as e:
        log.stop_timer()
        log.is_fallback = True
        openai_breaker.record_failure()
        log.emit()
        logger.warning("OpenAI 추천 이유 생성 실패 (예상치 못한 오류), fallback 사용: %s", e)
        return _fallback_reasons(recommended_decks)


def _is_patch_trend(deck_rank: int, decks: list[MetaDeck]) -> bool:
    """S/A 티어 덱만 패치 트렌드 표시."""
    for d in decks:
        if d.rank == deck_rank:
            return d.grade in _PATCH_TREND_GRADES
    return False


def _fallback_reasons(decks: list[MetaDeck]) -> list[DeckReason]:
    return [
        DeckReason(
            deck_rank=deck.rank,
            is_patch_trend=deck.grade in _PATCH_TREND_GRADES,
            reason="현재 메타 기반 추천 덱입니다.",
        )
        for deck in decks[:3]
    ]
