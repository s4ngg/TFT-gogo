"""
비동기 SQLAlchemy 엔진/세션 설정.

ai-server는 원래 완전히 stateless였으나(요청마다 백엔드가 데이터를 통째로
전달), 메타덱 임베딩을 캐싱하기 위해 처음으로 자체 DB 상태를 갖는다.
DB에 접근할 수 없어도 추천 자체는 동작해야 하므로, 이 모듈을 사용하는
쪽(app.services.embedding)에서 연결 실패를 잡아 폴백하는 것을 전제로 한다.
"""
from __future__ import annotations

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine

from app.core.config import settings

_engine: AsyncEngine | None = None
_sessionmaker: async_sessionmaker[AsyncSession] | None = None


def get_engine() -> AsyncEngine:
    """
    pgvector.sqlalchemy.Vector가 bind/result processor에서 자체적으로
    텍스트 포맷(vector 컬럼의 기본 입력 포맷) 변환을 처리하므로,
    asyncpg에 별도 바이너리 codec(register_vector)을 등록하지 않는다.
    등록하면 이미 문자열로 변환된 값을 다시 바이너리 인코딩하려다
    충돌한다 (raw asyncpg 전용 패턴과 SQLAlchemy 패턴은 서로 다름).
    """
    global _engine
    if _engine is None:
        _engine = create_async_engine(settings.database_url, pool_pre_ping=True)
    return _engine


def get_sessionmaker() -> async_sessionmaker[AsyncSession]:
    global _sessionmaker
    if _sessionmaker is None:
        _sessionmaker = async_sessionmaker(get_engine(), expire_on_commit=False)
    return _sessionmaker


@asynccontextmanager
async def session_scope() -> AsyncIterator[AsyncSession]:
    async with get_sessionmaker()() as session:
        yield session
