"""
IP 기반 인메모리 Rate Limiter.

슬라이딩 윈도우 방식으로 IP별 요청 수를 제한한다.
FastAPI 미들웨어로 등록하여 AI 엔드포인트를 보호한다.
"""
import logging
import time
from collections import defaultdict

from fastapi import HTTPException, Request
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.responses import Response

from app.core.config import settings

logger = logging.getLogger(__name__)


class _TokenBucket:
    __slots__ = ("tokens", "last_refill")

    def __init__(self, max_tokens: int) -> None:
        self.tokens = float(max_tokens)
        self.last_refill = time.monotonic()


_buckets: dict[str, _TokenBucket] = defaultdict(
    lambda: _TokenBucket(settings.rate_limit_requests)
)


def _check_rate_limit(client_ip: str) -> None:
    now = time.monotonic()
    bucket = _buckets[client_ip]

    elapsed = now - bucket.last_refill
    refill = elapsed * (settings.rate_limit_requests / settings.rate_limit_window)
    bucket.tokens = min(settings.rate_limit_requests, bucket.tokens + refill)
    bucket.last_refill = now

    if bucket.tokens < 1:
        logger.warning("Rate limit 초과: ip=%s", client_ip)
        raise HTTPException(
            status_code=429,
            detail="요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
        )
    bucket.tokens -= 1


class RateLimitMiddleware(BaseHTTPMiddleware):
    """AI 엔드포인트(/api/chat, /api/analyze)에 대해 IP 기반 rate limit 적용."""

    _TARGET_PREFIXES = ("/api/chat", "/api/analyze")

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        if any(request.url.path.startswith(p) for p in self._TARGET_PREFIXES):
            client_ip = request.client.host if request.client else "unknown"
            _check_rate_limit(client_ip)
        return await call_next(request)
