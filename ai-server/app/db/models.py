"""
ai-server가 소유하는 유일한 영속 테이블: 메타덱 임베딩 캐시.

메타덱 자체는 Spring 백엔드 MySQL이 진짜 소유자다. ai-server는 매 요청마다
백엔드가 보내주는 MetaDeck(rank/grade/trait_suffixes/...)을 받을 뿐 별도
식별자가 없으므로, trait_suffixes 조합을 정규화한 해시(signature)를 키로
써서 "같은 시너지 조합이면 임베딩을 재사용"하는 캐시 테이블로 둔다.

signature만으로는 부족하다: EMBEDDING_MODEL을 나중에 바꾸면 옛 모델로 만든
덱 벡터와 새 모델로 만든 플레이어 벡터를 같은 코사인 공간인 것처럼 비교하게
되어 추천이 조용히 오염된다. 그래서 (signature, model) 복합키로 모델별 캐시를
분리한다 — 모델을 바꾸면 새 (signature, model) 조합이 새로 채워지고, 이전
모델의 행은 조회 대상에서 자연히 제외된다(더 이상 매칭되지 않을 뿐 삭제되진
않으므로, 모델을 자주 바꾸면 별도로 정리가 필요하다).
"""
from datetime import datetime, timezone

from pgvector.sqlalchemy import Vector
from sqlalchemy import DateTime, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

# 스키마 차원. settings(.env)로 바꿀 수 있는 값이 아니라 이 테이블의 실제 컬럼
# 타입에 고정된 상수다 — alembic 마이그레이션도 반드시 이 값을 그대로 가져다 쓴다.
# 바꾸려면: 새 alembic 마이그레이션(+ 기존 캐시 행 폐기 또는 재임베딩)이 필요하다.
EMBEDDING_DIMENSIONS = 256


class Base(DeclarativeBase):
    pass


class MetaDeckEmbedding(Base):
    __tablename__ = "meta_deck_embedding"

    signature: Mapped[str] = mapped_column(String(64), primary_key=True)
    model: Mapped[str] = mapped_column(String(64), primary_key=True)
    embedding: Mapped[list[float]] = mapped_column(Vector(EMBEDDING_DIMENSIONS))
    source_text: Mapped[str] = mapped_column(String(512))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
