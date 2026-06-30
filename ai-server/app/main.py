import logging
import sys

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import analyze, chat
from app.core.config import settings
from app.core.rate_limiter import RateLimitMiddleware

sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    stream=sys.stdout,
)

app = FastAPI(
    title="TFTgogo AI Server",
    description="TFT 전적 분석 및 덱 추천 AI 서비스",
    version="0.1.0",
    docs_url="/docs" if settings.app_env != "production" else None,
)

app.add_middleware(RateLimitMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allowed_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(analyze.router, prefix="/api")
app.include_router(chat.router, prefix="/api")


@app.get("/health")
def health():
    return {"status": "ok", "env": settings.app_env}
