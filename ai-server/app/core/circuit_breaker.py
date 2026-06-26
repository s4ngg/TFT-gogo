"""
OpenAI 호출용 Circuit Breaker.

슬라이딩 윈도우 내 연속 실패 횟수가 임계치를 넘으면
일정 시간 동안 OpenAI 호출을 차단하고 fallback만 반환한다.

NOTE: 인메모리 상태이므로 다중 워커(uvicorn --workers N) 환경에서는
워커별로 독립 상태를 가진다. 실효 임계치가 threshold × 워커 수로
늘어날 수 있으며, 현재는 단일 워커 배포를 전제한다.
"""
import logging
import time

from app.core.config import settings

logger = logging.getLogger(__name__)


class CircuitBreaker:
    def __init__(self) -> None:
        self._failures: list[float] = []
        self._tripped_at: float | None = None

    def is_open(self) -> bool:
        """차단 상태이면 True."""
        if self._tripped_at is None:
            return False
        elapsed = time.monotonic() - self._tripped_at
        if elapsed >= settings.circuit_breaker_cooldown:
            logger.info("Circuit breaker 쿨다운 종료 — half-open 전환")
            self._tripped_at = None
            self._failures.clear()
            return False
        return True

    def record_success(self) -> None:
        self._failures.clear()
        self._tripped_at = None

    def record_failure(self) -> None:
        now = time.monotonic()
        cutoff = now - settings.circuit_breaker_window
        self._failures = [t for t in self._failures if t > cutoff]
        self._failures.append(now)

        if len(self._failures) >= settings.circuit_breaker_threshold:
            self._tripped_at = time.monotonic()
            logger.warning(
                "Circuit breaker OPEN: %d회 실패 (윈도우 %ds)",
                len(self._failures),
                settings.circuit_breaker_window,
            )


openai_breaker = CircuitBreaker()
