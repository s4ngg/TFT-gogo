"""
recommender.py 단위 테스트

외부 OpenAI 호출 없이 mock으로 fallback 경로와 랭킹 로직을 검증.
"""
import json
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from openai import APITimeoutError, APIConnectionError, APIStatusError

from app.models.match import MetaDeck, TraitStat
from app.services import embedding
from app.services.recommender import (
    _fallback_reasons,
    _is_patch_trend,
    _match_score,
    generate_reasons,
    rank_meta_decks,
    rank_meta_decks_semantic,
)

_BUDGET_PATH = "app.services.recommender.check_budget"
_BREAKER_PATH = "app.services.recommender.openai_breaker"


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


# ── rank_meta_decks_semantic (set-overlap + pgvector 하이브리드) ─────────

@pytest.mark.asyncio
async def test_rank_meta_decks_semantic_벡터_실패시_matchscore와_동일_순서():
    good = [_trait("Warrior", avg_place="2.5"), _trait("Mage")]
    deck_match = _deck(2, "A", ["warrior", "mage", "assassin"])
    deck_no_match = _deck(1, "S", ["tank", "bruiser"])

    with patch(
        "app.services.recommender.embedding.ensure_deck_embeddings_cached",
        AsyncMock(return_value=False),
    ):
        ranked = await rank_meta_decks_semantic([deck_no_match, deck_match], good, [])

    fallback_ranked = rank_meta_decks([deck_no_match, deck_match], good)
    assert [d.rank for d in ranked] == [d.rank for d in fallback_ranked]


@pytest.mark.asyncio
async def test_rank_meta_decks_semantic_플레이어_임베딩_실패시_matchscore와_동일_순서():
    good = [_trait("Warrior", avg_place="2.5")]
    deck_match = _deck(2, "A", ["warrior"])
    deck_no_match = _deck(1, "S", ["tank"])

    with patch(
        "app.services.recommender.embedding.ensure_deck_embeddings_cached",
        AsyncMock(return_value=True),
    ), patch(
        "app.services.recommender.embedding.embed_texts",
        AsyncMock(return_value=None),
    ):
        ranked = await rank_meta_decks_semantic([deck_no_match, deck_match], good, [])

    assert ranked[0].rank == 2


@pytest.mark.asyncio
async def test_rank_meta_decks_semantic_벡터_유사도가_동점_덱_순서를_바꾼다():
    good = [_trait("Warrior", avg_place="2.5")]
    # 둘 다 good_traits와 겹치는 시너지가 없어 _match_score는 0으로 동점
    deck_a = _deck(1, "S", ["tank"])
    deck_b = _deck(2, "A", ["bruiser"])

    sig_a = embedding.deck_signature(deck_a)
    sig_b = embedding.deck_signature(deck_b)

    async def fake_similarity_scores(query_vector, signatures):
        return {sig_a: 0.1, sig_b: 0.9}

    with patch(
        "app.services.recommender.embedding.ensure_deck_embeddings_cached",
        AsyncMock(return_value=True),
    ), patch(
        "app.services.recommender.embedding.embed_texts",
        AsyncMock(return_value=[[0.0]]),
    ), patch(
        "app.services.recommender.embedding.similarity_scores",
        fake_similarity_scores,
    ):
        ranked = await rank_meta_decks_semantic([deck_a, deck_b], good, [])

    # match_score는 동점(0)이므로 유사도가 더 높은 deck_b(rank=2)가 앞으로 온다
    assert ranked[0].rank == 2


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
         patch("app.services.recommender._get_client") as mock_get_client, \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
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
         patch("app.services.recommender._get_client") as mock_get_client, \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
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
         patch("app.services.recommender._get_client") as mock_get_client, \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
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
         patch("app.services.recommender._get_client") as mock_get_client, \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
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
         patch("app.services.recommender._get_client") as mock_get_client, \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
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
