"""create meta_deck_embedding table + pgvector extension

Revision ID: 0001
Revises:
Create Date: 2026-07-09
"""
from alembic import op
import sqlalchemy as sa
from pgvector.sqlalchemy import Vector

from app.core.config import settings

# revision identifiers, used by Alembic.
revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    op.create_table(
        "meta_deck_embedding",
        sa.Column("signature", sa.String(length=64), primary_key=True),
        sa.Column("embedding", Vector(settings.embedding_dimensions), nullable=False),
        sa.Column("source_text", sa.String(length=512), nullable=False),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
    )


def downgrade() -> None:
    op.drop_table("meta_deck_embedding")
    op.execute("DROP EXTENSION IF EXISTS vector")
