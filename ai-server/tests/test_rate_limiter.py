"""
rate_limiter.py 단위 테스트

given / when / then 패턴으로 작성.
전역 토큰 버킷의 허용/차단과 미들웨어의 429 응답을 검증한다.
"""
import pytest
from unittest.mock import patch, AsyncMock, MagicMock

from starlette.testclient import TestClient
from fastapi import FastAPI

from app.core.rate_limiter import (
    RateLimitMiddleware,
    _check_rate_limit,
    _TokenBucket,
    _global_bucket,
)
import app.core.rate_limiter as rl_module


def _reset_bucket():
    """테스트 간 전역 버킷 초기화."""
    rl_module._global_bucket = None


def _settings(requests: int = 30, window: int = 60):
    return type("S", (), {
        "rate_limit_requests": requests,
        "rate_limit_window": window,
    })()


# ── _check_rate_limit ───────────────────────────────────────────────────

def test_토큰_잔량_있으면_True():
    _reset_bucket()
    with patch("app.core.rate_limiter.settings", _settings(requests=5)):
        assert _check_rate_limit() is True


def test_토큰_소진_시_False():
    _reset_bucket()
    with patch("app.core.rate_limiter.settings", _settings(requests=2, window=60)):
        assert _check_rate_limit() is True
        assert _check_rate_limit() is True
        assert _check_rate_limit() is False


def test_시간_경과_후_토큰_리필():
    _reset_bucket()
    with patch("app.core.rate_limiter.settings", _settings(requests=1, window=1)):
        assert _check_rate_limit() is True
        assert _check_rate_limit() is False
        # 버킷의 last_refill을 과거로 조작하여 리필 시뮬레이션
        bucket = rl_module._global_bucket
        import time
        bucket.last_refill = time.monotonic() - 2
        assert _check_rate_limit() is True


# ── 미들웨어 통합 테스트 ────────────────────────────────────────────────

def _create_test_app() -> FastAPI:
    app = FastAPI()
    app.add_middleware(RateLimitMiddleware)

    @app.get("/api/chat")
    def chat():
        return {"reply": "ok"}

    @app.get("/api/analyze")
    def analyze():
        return {"result": "ok"}

    @app.get("/health")
    def health():
        return {"status": "ok"}

    return app


def test_대상_경로_rate_limit_초과_시_429():
    _reset_bucket()
    app = _create_test_app()
    client = TestClient(app)

    with patch("app.core.rate_limiter.settings", _settings(requests=1)):
        resp1 = client.get("/api/chat")
        assert resp1.status_code == 200

        resp2 = client.get("/api/chat")
        assert resp2.status_code == 429
        body = resp2.json()
        assert body["success"] is False
        assert body["code"] == "RATE_LIMIT_EXCEEDED"
        assert body["data"] is None


def test_비대상_경로는_rate_limit_미적용():
    _reset_bucket()
    app = _create_test_app()
    client = TestClient(app)

    with patch("app.core.rate_limiter.settings", _settings(requests=1)):
        # /health는 rate limit 대상이 아님
        for _ in range(5):
            resp = client.get("/health")
            assert resp.status_code == 200
