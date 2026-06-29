"""
OpenAI 호출 전 입력 토큰 예산 검증.

tiktoken으로 메시지 토큰 수를 추정하고, 기능별 상한을 초과하면
HTTPException(413)을 발생시켜 OpenAI 호출 없이 차단한다.
"""
import logging

import tiktoken
from fastapi import HTTPException

from app.core.config import settings

logger = logging.getLogger(__name__)

_encoding: tiktoken.Encoding | None = None
_encoding_model: str | None = None


def _get_encoding() -> tiktoken.Encoding:
    global _encoding, _encoding_model
    model = settings.openai_model
    if _encoding is None or _encoding_model != model:
        _encoding = tiktoken.encoding_for_model(model)
        _encoding_model = model
    return _encoding


def estimate_tokens(messages: list[dict]) -> int:
    """OpenAI chat messages 리스트의 토큰 수를 추정한다."""
    enc = _get_encoding()
    total = 0
    for msg in messages:
        total += 4  # role/content 구조 오버헤드
        total += len(enc.encode(msg.get("content", "")))
    total += 2  # assistant reply priming
    return total


def check_budget(messages: list[dict], max_tokens: int, feature: str) -> int:
    """
    토큰 예산을 검증한다. 초과 시 413 HTTPException 발생.
    반환값: 추정 토큰 수 (로깅용).
    """
    estimated = estimate_tokens(messages)
    if estimated > max_tokens:
        logger.warning(
            "[%s] 입력 토큰 예산 초과: estimated=%d, max=%d",
            feature, estimated, max_tokens,
        )
        raise HTTPException(
            status_code=413,
            detail=f"입력 토큰이 허용 한도를 초과했습니다 (예상: {estimated}, 한도: {max_tokens})",
        )
    return estimated
