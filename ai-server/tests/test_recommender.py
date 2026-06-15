"""
recommender.py 단위 테스트

외부 OpenAI 호출 없이 mock으로 fallback 경로와 랭킹 로직을 검증.
"""
import json
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from openai import APITimeoutError, APIConnectionError, APIStatusError

from app.models.match import DeckReason, MetaDeck, TraitStat
from app.services.recommender import (
    _fallback_reasons,
    _is_patch_trend,
    _match_score,
    generate_reasons,
    rank_meta_decks,
)


# ── 픽스처 ──────────────────────────────────────────────────────────────

def _deck(rank: int, grade: str, trait_suffixes: list[str]) -> MetaDeck:
    return MetaDeck(
        rank=rank,
        grade=grade,
        trait_suffixes=trait_suffixes,
        avg_place="3.5",
        top4_rate="60%",
        pick_rate="5%",
    )


def _trait(name: str, avg_place: str = "3.0", top4_rate: str = "65%") -> TraitStat:
    return TraitStat(
        name=name,
        avg_place=avg_place,
        top4_rate=top4_rate,
        games=10,
        count=4,
        icon_url="",
        tone="gold",
    )


# ── rank_meta_decks ──────────────────────────────────────────────────────

def test_rank_meta_decks_겹치는_시너지가_많은_덱을_상위로_정렬한다():
    # given
    good = [_trait("Warrior", avg_place="2.5"), _trait("Mage")]
    deck_match = _deck(2, "A", ["warrior", "mage", "assassin"])
    deck_no_match = _deck(1, "S", ["tank", "bruiser"])

    # when
    ranked = rank_meta_decks([deck_no_match, deck_match], good)

    # then — 겹치는 시너지가 있는 덱이 앞에 위치
    assert ranked[0].rank == 2


def test_rank_meta_decks_겹치는_시너지_없으면_score_0으로_처리한다():
    good = [_trait("Warrior")]
    decks = [_deck(1, "S", ["mage"]), _deck(2, "A", ["tank"])]
    ranked = rank_meta_decks(decks, good)
    # 모두 score 0이므로 원래 순서 유지 (stable sort)
    assert len(ranked) == 2


# ── _match_score ─────────────────────────────────────────────────────────

def test_match_score_겹치는_시너지_없으면_0():
    deck = _deck(1, "S", ["mage", "tank"])
    good = [_trait("warrior")]
    assert _match_score(deck, good) == 0.0


def test_match_score_평균등수_낮을수록_높은_점수():
    deck = _deck(1, "S", ["warrior"])
    good_low = [_trait("warrior", avg_place="2.0")]
    good_high = [_trait("warrior", avg_place="5.0")]
    assert _match_score(deck, good_low) > _match_score(deck, good_high)


# ── _is_patch_trend ──────────────────────────────────────────────────────

def test_is_patch_trend_S_A_티어만_true():
    decks = [_deck(1, "S", []), _deck(2, "A", []), _deck(3, "B", []), _deck(4, "C", [])]
    assert _is_patch_trend(1, decks) is True
    assert _is_patch_trend(2, decks) is True
    assert _is_patch_trend(3, decks) is False
    assert _is_patch_trend(4, decks) is False


def test_is_patch_trend_덱_없으면_False():
    assert _is_patch_trend(99, []) is False


# ── _fallback_reasons ────────────────────────────────────────────────────

def test_fallback_reasons_최대_3개_반환():
    decks = [_deck(i, "S", []) for i in range(1, 6)]
    reasons = _fallback_reasons(decks)
    assert len(reasons) == 3


def test_fallback_reasons_S_A_티어_isPatchTrend_true():
    decks = [_deck(1, "S", []), _deck(2, "B", [])]
    reasons = _fallback_reasons(decks)
    assert reasons[0].is_patch_trend is True
    assert reasons[1].is_patch_trend is False


# ── generate_reasons — OpenAI 미설정 시 fallback ─────────────────────────

@pytest.mark.asyncio
async def test_generate_reasons_api_key_없으면_fallback():
    decks = [_deck(1, "S", ["warrior"])]
    with patch("app.services.recommender.settings") as mock_settings:
        mock_settings.openai_api_key = None
        result = await generate_reasons([], [], decks, "stats")
    assert len(result) == 1
    assert result[0].reason == "현재 메타 기반 추천 덱입니다."


# ── generate_reasons — OpenAI 오류 fallback ──────────────────────────────

def _mock_openai_response(content: str):
    msg = MagicMock()
    msg.content = content
    choice = MagicMock()
    choice.message = msg
    resp = MagicMock()
    resp.choices = [choice]
    return resp


@pytest.mark.asyncio
async def test_generate_reasons_json_파싱_실패시_fallback():
    decks = [_deck(1, "A", ["mage"])]
    with patch("app.services.recommender.settings") as mock_settings, \
         patch("app.services.recommender._get_client") as mock_get_client:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        client = AsyncMock()
        client.chat.completions.create = AsyncMock(
            return_value=_mock_openai_response("not valid json {{{{")
        )
        mock_get_client.return_value = client

        result = await generate_reasons([], [], decks, "stats")

    assert len(result) == 1
    assert result[0].reason == "현재 메타 기반 추천 덱입니다."


@pytest.mark.asyncio
async def test_generate_reasons_timeout_시_fallback():
    decks = [_deck(1, "S", [])]
    with patch("app.services.recommender.settings") as mock_settings, \
         patch("app.services.recommender._get_client") as mock_get_client:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        client = AsyncMock()
        client.chat.completions.create = AsyncMock(
            side_effect=APITimeoutError(request=MagicMock())
        )
        mock_get_client.return_value = client

        result = await generate_reasons([], [], decks, "stats")

    assert len(result) == 1


@pytest.mark.asyncio
async def test_generate_reasons_connection_error_시_fallback():
    decks = [_deck(1, "A", [])]
    with patch("app.services.recommender.settings") as mock_settings, \
         patch("app.services.recommender._get_client") as mock_get_client:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        client = AsyncMock()
        client.chat.completions.create = AsyncMock(
            side_effect=APIConnectionError(request=MagicMock())
        )
        mock_get_client.return_value = client

        result = await generate_reasons([], [], decks, "stats")

    assert len(result) == 1


@pytest.mark.asyncio
async def test_generate_reasons_status_error_시_fallback():
    decks = [_deck(1, "B", [])]
    with patch("app.services.recommender.settings") as mock_settings, \
         patch("app.services.recommender._get_client") as mock_get_client:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        client = AsyncMock()
        client.chat.completions.create = AsyncMock(
            side_effect=APIStatusError(
                "rate limit", response=MagicMock(status_code=429), body={}
            )
        )
        mock_get_client.return_value = client

        result = await generate_reasons([], [], decks, "stats")

    assert len(result) == 1


@pytest.mark.asyncio
async def test_generate_reasons_정상_응답_파싱():
    decks = [_deck(1, "S", ["warrior"]), _deck(2, "A", ["mage"])]
    raw = json.dumps([
        {"deck_rank": 1, "reason": "테스트 추천 이유 1"},
        {"deck_rank": 2, "reason": "테스트 추천 이유 2"},
    ])
    with patch("app.services.recommender.settings") as mock_settings, \
         patch("app.services.recommender._get_client") as mock_get_client:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        client = AsyncMock()
        client.chat.completions.create = AsyncMock(
            return_value=_mock_openai_response(raw)
        )
        mock_get_client.return_value = client

        result = await generate_reasons([], [], decks, "stats")

    assert len(result) == 2
    assert result[0].deck_rank == 1
    assert result[0].reason == "테스트 추천 이유 1"
    assert result[0].is_patch_trend is True  # S 티어
    assert result[1].is_patch_trend is True   # A 티어
