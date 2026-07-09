"""
ai-server가 소유하는 유일한 영속 테이블: 메타덱 임베딩 캐시.

메타덱 자체는 Spring 백엔드 MySQL이 진짜 소유자다. ai-server는 매 요청마다
백엔드가 보내주는 MetaDeck(rank/grade/trait_suffixes/...)을 받을 뿐 별도
식별자가 없으므로, trait_suffixes 조합을 정규화한 해시(signature)를 키로
써서 "같은 시너지 조합이면 임베딩을 재사용"하는 캐시 테이블로 둔다.
"""
from datetime import datetime, timezone

from pgvector.sqlalchemy import Vector
from sqlalchemy import DateTime, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

from app.core.config import settings


class Base(DeclarativeBase):
    pass


class MetaDeckEmbedding(Base):
    __tablename__ = "meta_deck_embedding"

    signature: Mapped[str] = mapped_column(String(64), primary_key=True)
    embedding: Mapped[list[float]] = mapped_column(Vector(settings.embedding_dimensions))
    source_text: Mapped[str] = mapped_column(String(512))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
