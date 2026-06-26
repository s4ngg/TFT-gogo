"""
AI 요청/응답 추적 로거.

requestId, 모델, 입력/출력 토큰, 지연시간, fallback 여부를 기록한다.
"""
import logging
import time
import uuid
from dataclasses import dataclass, field

logger = logging.getLogger("ai_request")


@dataclass
class AiRequestLog:
    feature: str
    model: str
    input_tokens_estimated: int = 0
    output_tokens: int = 0
    latency_ms: int = 0
    is_fallback: bool = False
    request_id: str = field(default_factory=lambda: uuid.uuid4().hex[:12])
    _start: float = field(default_factory=time.monotonic, repr=False)

    def start_timer(self) -> None:
        self._start = time.monotonic()

    def stop_timer(self) -> None:
        self.latency_ms = int((time.monotonic() - self._start) * 1000)

    def emit(self) -> None:
        logger.info(
            "request_id=%s feature=%s model=%s "
            "input_tokens=%d output_tokens=%d "
            "latency_ms=%d fallback=%s",
            self.request_id,
            self.feature,
            self.model,
            self.input_tokens_estimated,
            self.output_tokens,
            self.latency_ms,
            self.is_fallback,
        )
