"""
메타 매칭 + OpenAI 추천 이유 생성 서비스

내 플레이 스타일(시너지 통계)과 현재 메타 덱을 매칭해
추천 이유를 OpenAI로 생성한다.
"""
import json
import logging
import re

from openai import AsyncOpenAI, APIStatusError, APITimeoutError, APIConnectionError

from app.core.config import settings
from app.models.match import DeckReason, MetaDeck, TraitStat

logger = logging.getLogger(__name__)

_client: AsyncOpenAI | None = None

# 추천 이유 1건당 예상 토큰 (한국어 1~2문장 + JSON 키)
_TOKENS_PER_DECK = 80
_TOKENS_BASE = 50  # JSON 배열 오버헤드


def _get_client() -> AsyncOpenAI:
    global _client
    if _client is None:
        _client = AsyncOpenAI(api_key=settings.openai_api_key)
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
    good_trait_names = [_sanitize(t.name) for t in good_traits[:5]]
    deck_info = [
        {
            "rank": d.rank,
            "grade": _sanitize(d.grade),
            "traits": [_sanitize(s) for s in d.trait_suffixes[:4]],
        }
        for d in target_decks
    ]

    # system/user 분리: 역할 지시는 system, 데이터는 user
    system_message = (
        "당신은 TFT(팀파이트 택티스) 전문 코치입니다. "
        "플레이어 데이터를 바탕으로 추천 덱 이유를 한국어 1~2문장으로 작성합니다. "
        "반드시 아래 JSON 배열 형식만 출력하세요. 다른 텍스트는 절대 출력하지 마세요:\n"
        '[{"deck_rank": <숫자>, "reason": "<추천 이유>"}]'
    )
    user_message = (
        f"플레이어 최근 통계: {_sanitize(stats_summary)}\n"
        f"잘 맞는 시너지: {', '.join(good_trait_names)}\n\n"
        f"추천 덱 목록:\n{json.dumps(deck_info, ensure_ascii=False)}"
    )

    # 추천 덱 수에 비례해 max_tokens 동적 계산
    max_tokens = _TOKENS_BASE + len(target_decks) * _TOKENS_PER_DECK

    try:
        client = _get_client()
        response = await client.chat.completions.create(
            model=settings.openai_model,
            messages=[
                {"role": "system", "content": system_message},
                {"role": "user", "content": user_message},
            ],
            temperature=0.3,
            max_tokens=max_tokens,
        )
        content = response.choices[0].message.content or ""
        content = re.sub(r"```json\s*|\s*```", "", content).strip()
        raw = json.loads(content)
        return [
            DeckReason(
                deck_rank=item["deck_rank"],
                is_patch_trend=False,
                reason=item["reason"],
            )
            for item in raw
        ]
    except json.JSONDecodeError as e:
        logger.warning("OpenAI 응답 JSON 파싱 실패, fallback 사용: %s", e)
        return _fallback_reasons(recommended_decks)
    except APITimeoutError as e:
        logger.warning("OpenAI API 타임아웃, fallback 사용: %s", e)
        return _fallback_reasons(recommended_decks)
    except APIConnectionError as e:
        logger.warning("OpenAI API 연결 실패, fallback 사용: %s", e)
        return _fallback_reasons(recommended_decks)
    except APIStatusError as e:
        logger.warning("OpenAI API 오류 status=%s, fallback 사용: %s", e.status_code, e)
        return _fallback_reasons(recommended_decks)
    except Exception as e:
        logger.warning("OpenAI 추천 이유 생성 실패 (예상치 못한 오류), fallback 사용: %s", e)
        return _fallback_reasons(recommended_decks)


def _fallback_reasons(decks: list[MetaDeck]) -> list[DeckReason]:
    return [
        DeckReason(
            deck_rank=deck.rank,
            is_patch_trend=deck.grade == "S",
            reason="현재 메타 기반 추천 덱입니다.",
        )
        for deck in decks[:3]
    ]
