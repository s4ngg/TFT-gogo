"""
메타덱/플레이어 스타일 임베딩 생성 + pgvector 코사인 유사도 조회.

OpenAI Embeddings API로 텍스트를 벡터로 바꾸고, 메타덱 임베딩은
trait_suffixes 조합 해시(signature)를 키로 Postgres(pgvector)에 캐싱한다.
메타덱은 하루 1회 배치 집계 때만 구성이 바뀌므로, 같은 조합이면 임베딩을
재사용해 OpenAI 호출을 줄인다.

DB나 OpenAI 호출 중 하나라도 실패하면 None을 반환해 호출부
(app.services.recommender)가 기존 set-overlap 스코어링만으로
폴백하도록 한다 — 벡터 검색은 어디까지나 보강 신호이지 필수 경로가 아니다.
"""
import hashlib
import logging

from openai import AsyncOpenAI, APIConnectionError, APIStatusError, APITimeoutError
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert

from app.core.ai_logger import AiRequestLog
from app.core.circuit_breaker import CircuitBreaker
from app.core.config import settings
from app.db.models import EMBEDDING_DIMENSIONS, MetaDeckEmbedding
from app.db.session import session_scope
from app.models.match import MetaDeck, TraitStat

logger = logging.getLogger(__name__)

embedding_breaker = CircuitBreaker()

_client: AsyncOpenAI | None = None


def _get_client() -> AsyncOpenAI:
    global _client
    if _client is None:
        _client = AsyncOpenAI(
            api_key=settings.openai_api_key,
            timeout=settings.embedding_timeout,
        )
    return _client


def deck_signature(deck: MetaDeck) -> str:
    """trait_suffixes 조합 기준 결정적 해시. 순서·대소문자와 무관하게 같은 조합이면 같은 값."""
    normalized = ",".join(sorted(s.lower() for s in deck.trait_suffixes))
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def deck_embedding_text(deck: MetaDeck) -> str:
    traits = ", ".join(deck.trait_suffixes) if deck.trait_suffixes else "구성 정보 없음"
    return f"TFT 메타덱 시너지 구성: {traits}"


def player_style_text(good_traits: list[TraitStat], bad_traits: list[TraitStat]) -> str:
    good = ", ".join(t.name for t in good_traits) if good_traits else "없음"
    bad = ", ".join(t.name for t in bad_traits) if bad_traits else "없음"
    return f"플레이어가 잘 다루는 시너지: {good}. 잘 안 맞는 시너지: {bad}"


async def embed_texts(texts: list[str]) -> list[list[float]] | None:
    """OpenAI Embeddings API 호출. API 키 미설정/오류/circuit open 시 None."""
    if not texts:
        return []
    if not settings.openai_api_key:
        return None
    if embedding_breaker.is_open():
        logger.warning("[embedding] circuit breaker OPEN — 벡터 검색 스킵")
        return None

    log = AiRequestLog(feature="embedding", model=settings.embedding_model)
    log.start_timer()
    try:
        client = _get_client()
        response = await client.embeddings.create(
            model=settings.embedding_model,
            input=texts,
            # DB 컬럼 차원(app.db.models.EMBEDDING_DIMENSIONS)과 반드시 일치해야
            # 하므로 settings가 아니라 스키마 상수를 그대로 쓴다.
            dimensions=EMBEDDING_DIMENSIONS,
        )
        log.stop_timer()
        if response.usage:
            log.output_tokens = response.usage.total_tokens
        embedding_breaker.record_success()
        log.emit()
        return [item.embedding for item in response.data]
    except (APITimeoutError, APIConnectionError, APIStatusError) as e:
        log.stop_timer()
        log.is_fallback = True
        embedding_breaker.record_failure()
        log.emit()
        logger.warning("OpenAI 임베딩 API 오류, 벡터 검색 스킵: %s", e)
        return None
    except Exception as e:
        log.stop_timer()
        log.is_fallback = True
        embedding_breaker.record_failure()
        log.emit()
        logger.warning("임베딩 생성 실패(예상치 못한 오류), 벡터 검색 스킵: %s", e)
        return None


async def ensure_deck_embeddings_cached(meta_decks: list[MetaDeck]) -> bool:
    """
    meta_decks의 임베딩이 전부 pgvector 테이블에 있는 상태로 만든다.
    없는 것만 새로 임베딩해서 upsert. DB/OpenAI 중 하나라도 실패하면 False.
    """
    if not meta_decks:
        return True

    signature_map = {deck_signature(d): d for d in meta_decks}

    try:
        async with session_scope() as session:
            existing = (
                await session.execute(
                    select(MetaDeckEmbedding.signature).where(
                        MetaDeckEmbedding.signature.in_(signature_map.keys())
                    )
                )
            ).scalars().all()

            missing_signatures = [sig for sig in signature_map if sig not in set(existing)]
            if not missing_signatures:
                return True

            texts = [deck_embedding_text(signature_map[sig]) for sig in missing_signatures]
            vectors = await embed_texts(texts)
            if vectors is None:
                return False

            for sig, vector in zip(missing_signatures, vectors):
                stmt = (
                    pg_insert(MetaDeckEmbedding)
                    .values(
                        signature=sig,
                        embedding=vector,
                        source_text=deck_embedding_text(signature_map[sig]),
                    )
                    .on_conflict_do_nothing(index_elements=["signature"])
                )
                await session.execute(stmt)

            await session.commit()
            return True
    except Exception as e:
        logger.warning("메타덱 임베딩 캐시 조회/저장 실패, 벡터 검색 스킵: %s", e)
        return False


async def similarity_scores(
    query_vector: list[float], signatures: list[str]
) -> dict[str, float] | None:
    """
    pgvector 코사인 거리(<=> 연산자)로 signature별 유사도(0~1, 높을수록 유사)를 계산.
    ensure_deck_embeddings_cached()로 대상 signature가 이미 저장돼 있다는 전제.
    """
    if not signatures:
        return {}
    try:
        async with session_scope() as session:
            distance = MetaDeckEmbedding.embedding.cosine_distance(query_vector)
            rows = (
                await session.execute(
                    select(MetaDeckEmbedding.signature, distance.label("distance")).where(
                        MetaDeckEmbedding.signature.in_(signatures)
                    )
                )
            ).all()
            return {sig: 1.0 - float(dist) for sig, dist in rows}
    except Exception as e:
        logger.warning("pgvector 유사도 조회 실패, 벡터 검색 스킵: %s", e)
        return None
