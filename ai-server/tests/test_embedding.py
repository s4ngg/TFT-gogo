"""
embedding.py 단위 테스트

실제 Postgres/OpenAI 없이 mock으로 캐시 hit/miss, 실패 시 폴백(None) 경로를 검증.
"""
from contextlib import asynccontextmanager
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from openai import APIConnectionError

from app.models.match import MetaDeck, TraitStat
from app.services import embedding

_BREAKER_PATH = "app.services.embedding.embedding_breaker"


def _deck(rank: int, trait_suffixes: list[str]) -> MetaDeck:
    return MetaDeck(
        rank=rank,
        grade="S",
        trait_suffixes=trait_suffixes,
        avg_place="3.5",
        top4_rate="60%",
        pick_rate="5%",
    )


def _session_scope_with(session):
    @asynccontextmanager
    async def _scope():
        yield session

    return _scope


def _raising_session_scope(exc: Exception):
    @asynccontextmanager
    async def _scope():
        raise exc
        yield  # pragma: no cover

    return _scope


def _scalars_result(values):
    result = MagicMock()
    result.scalars.return_value.all.return_value = values
    return result


def _rows_result(rows):
    result = MagicMock()
    result.all.return_value = rows
    return result


# ── deck_signature ────────────────────────────────────────────────────

def test_deck_signature_순서와_대소문자_무관하게_동일():
    a = _deck(1, ["Warrior", "mage"])
    b = _deck(2, ["mage", "warrior"])
    assert embedding.deck_signature(a) == embedding.deck_signature(b)


def test_deck_signature_다른_조합이면_다른_값():
    a = _deck(1, ["warrior"])
    b = _deck(1, ["mage"])
    assert embedding.deck_signature(a) != embedding.deck_signature(b)


# ── deck_embedding_text / player_style_text ─────────────────────────────

def test_deck_embedding_text_시너지_없으면_안내문구():
    deck = _deck(1, [])
    assert "구성 정보 없음" in embedding.deck_embedding_text(deck)


def test_player_style_text_good_bad_모두_포함():
    good = [TraitStat(name="Warrior", avg_place="2.0", top4_rate="70%", games=10, count=4, icon_url="", tone="gold")]
    bad = [TraitStat(name="Mage", avg_place="5.0", top4_rate="30%", games=10, count=2, icon_url="", tone="bronze")]
    text = embedding.player_style_text(good, bad)
    assert "Warrior" in text
    assert "Mage" in text


# ── embed_texts ───────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_embed_texts_빈_리스트면_빈_리스트_반환():
    assert await embedding.embed_texts([]) == []


@pytest.mark.asyncio
async def test_embed_texts_api_key_없으면_none():
    with patch("app.services.embedding.settings") as mock_settings:
        mock_settings.openai_api_key = ""
        result = await embedding.embed_texts(["hello"])
    assert result is None


@pytest.mark.asyncio
async def test_embed_texts_breaker_open이면_none():
    with patch("app.services.embedding.settings") as mock_settings, \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_breaker.is_open.return_value = True
        result = await embedding.embed_texts(["hello"])
    assert result is None


@pytest.mark.asyncio
async def test_embed_texts_정상_응답():
    item = MagicMock()
    item.embedding = [0.1, 0.2]
    response = MagicMock()
    response.data = [item]
    response.usage = MagicMock(total_tokens=5)

    with patch("app.services.embedding.settings") as mock_settings, \
         patch("app.services.embedding._get_client") as mock_get_client, \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.embedding_model = "text-embedding-3-small"
        mock_settings.embedding_dimensions = 2
        mock_breaker.is_open.return_value = False
        client = AsyncMock()
        client.embeddings.create = AsyncMock(return_value=response)
        mock_get_client.return_value = client

        result = await embedding.embed_texts(["deck text"])

    assert result == [[0.1, 0.2]]
    mock_breaker.record_success.assert_called_once()


@pytest.mark.asyncio
async def test_embed_texts_api_오류시_none_및_breaker_기록():
    with patch("app.services.embedding.settings") as mock_settings, \
         patch("app.services.embedding._get_client") as mock_get_client, \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "fake-key"
        mock_settings.embedding_model = "text-embedding-3-small"
        mock_settings.embedding_dimensions = 2
        mock_breaker.is_open.return_value = False
        client = AsyncMock()
        client.embeddings.create = AsyncMock(side_effect=APIConnectionError(request=MagicMock()))
        mock_get_client.return_value = client

        result = await embedding.embed_texts(["deck text"])

    assert result is None
    mock_breaker.record_failure.assert_called_once()


# ── ensure_deck_embeddings_cached ───────────────────────────────────────

@pytest.mark.asyncio
async def test_ensure_cached_빈_목록이면_true():
    assert await embedding.ensure_deck_embeddings_cached([]) is True


@pytest.mark.asyncio
async def test_ensure_cached_전부_이미_있으면_임베딩_호출_안함():
    decks = [_deck(1, ["warrior"]), _deck(2, ["mage"])]
    sigs = [embedding.deck_signature(d) for d in decks]
    session = MagicMock()
    session.execute = AsyncMock(return_value=_scalars_result(sigs))

    with patch("app.services.embedding.session_scope", _session_scope_with(session)), \
         patch("app.services.embedding.embed_texts") as mock_embed:
        result = await embedding.ensure_deck_embeddings_cached(decks)

    assert result is True
    mock_embed.assert_not_called()


@pytest.mark.asyncio
async def test_ensure_cached_없는것만_새로_임베딩():
    decks = [_deck(1, ["warrior"]), _deck(2, ["mage"])]
    existing_sig = embedding.deck_signature(decks[0])
    session = MagicMock()
    session.execute = AsyncMock(return_value=_scalars_result([existing_sig]))
    session.commit = AsyncMock()

    with patch("app.services.embedding.session_scope", _session_scope_with(session)), \
         patch("app.services.embedding.embed_texts", AsyncMock(return_value=[[0.1, 0.2]])):
        result = await embedding.ensure_deck_embeddings_cached(decks)

    assert result is True
    assert session.commit.await_count == 1


@pytest.mark.asyncio
async def test_ensure_cached_임베딩_실패시_false():
    decks = [_deck(1, ["warrior"])]
    session = MagicMock()
    session.execute = AsyncMock(return_value=_scalars_result([]))

    with patch("app.services.embedding.session_scope", _session_scope_with(session)), \
         patch("app.services.embedding.embed_texts", AsyncMock(return_value=None)):
        result = await embedding.ensure_deck_embeddings_cached(decks)

    assert result is False


@pytest.mark.asyncio
async def test_ensure_cached_db_예외시_false():
    decks = [_deck(1, ["warrior"])]

    with patch("app.services.embedding.session_scope", _raising_session_scope(RuntimeError("db down"))):
        result = await embedding.ensure_deck_embeddings_cached(decks)

    assert result is False


# ── similarity_scores ────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_similarity_scores_빈_signature면_빈_dict():
    assert await embedding.similarity_scores([0.1], []) == {}


@pytest.mark.asyncio
async def test_similarity_scores_코사인_거리를_유사도로_변환():
    session = MagicMock()
    session.execute = AsyncMock(return_value=_rows_result([("sig-a", 0.2), ("sig-b", 0.8)]))

    with patch("app.services.embedding.session_scope", _session_scope_with(session)):
        result = await embedding.similarity_scores([0.1, 0.2], ["sig-a", "sig-b"])

    assert result == pytest.approx({"sig-a": 0.8, "sig-b": 0.2})


@pytest.mark.asyncio
async def test_similarity_scores_예외시_none():
    with patch("app.services.embedding.session_scope", _raising_session_scope(RuntimeError("db down"))):
        result = await embedding.similarity_scores([0.1], ["sig-a"])

    assert result is None
