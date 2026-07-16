"""create meta_deck_embedding table + pgvector extension

Revision ID: 0001
Revises:
Create Date: 2026-07-09
"""
from alembic import op
import sqlalchemy as sa
from pgvector.sqlalchemy import Vector

# revision identifiers, used by Alembic.
revision = "0001"
down_revision = None
branch_labels = None
depends_on = None

# settings(.env)를 읽지 않고 고정 리터럴을 쓴다 — 마이그레이션은 실행 시점의
# 환경변수 값과 무관하게 항상 같은 스키마를 만들어야 한다. 반드시
# app/db/models.py의 EMBEDDING_DIMENSIONS와 같은 값으로 유지할 것.
# 값을 바꿔야 하면 이 상수가 아니라 새 revision을 추가한다.
_EMBEDDING_DIMENSIONS = 256


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    op.create_table(
        "meta_deck_embedding",
        # (signature, model) 복합 PK: EMBEDDING_MODEL을 바꾸면 이전 모델의
        # 캐시 행과 뒤섞이지 않도록 모델별로 캐시를 분리한다.
        sa.Column("signature", sa.String(length=64), primary_key=True),
        sa.Column("model", sa.String(length=64), primary_key=True),
        sa.Column("embedding", Vector(_EMBEDDING_DIMENSIONS), nullable=False),
        sa.Column("source_text", sa.String(length=512), nullable=False),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
    )


def downgrade() -> None:
    # vector 익스텐션은 DB 레벨 객체라 이 테이블 마이그레이션이 소유하지 않는다.
    # 다른 테이블/마이그레이션이 이미 vector를 쓰고 있을 수 있으므로 여기서
    # DROP EXTENSION을 실행하지 않는다 (CASCADE 없이는 실패하고, CASCADE를
    # 붙이면 무관한 다른 객체까지 깨질 수 있다).
    op.drop_table("meta_deck_embedding")
