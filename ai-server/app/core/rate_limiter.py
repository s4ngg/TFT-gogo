"""
전역 비용 보호 Rate Limiter.

AI 서버의 OpenAI 호출 총량을 제한한다.
호출자는 Spring backend 1대이므로 IP가 아닌 엔드포인트 단위 전역 버킷을 사용한다.
사용자별 제한은 backend의 AiChatRateLimiter(userId 기반)가 담당한다.
"""
import logging
import time

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.responses import JSONResponse, Response

from app.core.config import settings

logger = logging.getLogger(__name__)


class _TokenBucket:
    __slots__ = ("tokens", "last_refill")

    def __init__(self, max_tokens: int) -> None:
        self.tokens = float(max_tokens)
        self.last_refill = time.monotonic()


_global_bucket: _TokenBucket | None = None


def _get_bucket() -> _TokenBucket:
    global _global_bucket
    if _global_bucket is None:
        _global_bucket = _TokenBucket(settings.rate_limit_requests)
    return _global_bucket


def _check_rate_limit() -> bool:
    """전역 토큰이 남아 있으면 True, 소진되면 False."""
    bucket = _get_bucket()
    now = time.monotonic()

    elapsed = now - bucket.last_refill
    refill = elapsed * (settings.rate_limit_requests / settings.rate_limit_window)
    bucket.tokens = min(settings.rate_limit_requests, bucket.tokens + refill)
    bucket.last_refill = now

    if bucket.tokens < 1:
        logger.warning("전역 rate limit 초과")
        return False
    bucket.tokens -= 1
    return True


class RateLimitMiddleware(BaseHTTPMiddleware):
    """AI 엔드포인트에 대해 전역 비용 보호 rate limit 적용."""

    _TARGET_PREFIXES = ("/api/chat", "/api/analyze", "/api/gameguide/pathfinder")

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        if any(request.url.path.startswith(p) for p in self._TARGET_PREFIXES):
            if not _check_rate_limit():
                return JSONResponse(
                    status_code=429,
                    content={
                        "success": False,
                        "code": "RATE_LIMIT_EXCEEDED",
                        "message": "AI 서버 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.",
                        "data": None,
                    },
                )
        return await call_next(request)
