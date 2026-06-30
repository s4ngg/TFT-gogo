"""
token_budget.py 단위 테스트

given / when / then 패턴으로 작성.
토큰 추정과 예산 초과 시 413 반환을 검증한다.
"""
import pytest
from unittest.mock import patch

from fastapi import HTTPException

from app.core.token_budget import check_budget, estimate_tokens


# ── estimate_tokens ─────────────────────────────────────────────────────

def test_빈_메시지_리스트_최소_토큰():
    result = estimate_tokens([])
    # assistant reply priming(2) 만 존재
    assert result == 2


def test_단일_메시지_토큰_추정():
    messages = [{"role": "user", "content": "hello"}]
    result = estimate_tokens(messages)
    # 4(오버헤드) + encode("hello") + 2(priming) > 2
    assert result > 2


def test_여러_메시지_토큰_합산():
    messages = [
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "What is TFT?"},
    ]
    result = estimate_tokens(messages)
    # 2개 메시지 × 4 오버헤드 + 컨텐츠 토큰 + 2 priming
    assert result > 10


def test_content_없는_메시지_처리():
    messages = [{"role": "assistant"}]
    result = estimate_tokens(messages)
    # 4(오버헤드) + encode("") + 2(priming) = 6
    assert result == 6


# ── check_budget ────────────────────────────────────────────────────────

def test_예산_이내_시_추정_토큰_반환():
    messages = [{"role": "user", "content": "hi"}]
    result = check_budget(messages, max_tokens=10000, feature="test")
    assert isinstance(result, int)
    assert result > 0


def test_예산_초과_시_413_HTTPException():
    messages = [{"role": "user", "content": "a " * 5000}]
    with pytest.raises(HTTPException) as exc_info:
        check_budget(messages, max_tokens=10, feature="test")
    assert exc_info.value.status_code == 413


def test_설정_모델_변경_시_인코딩_갱신():
    import app.core.token_budget as tb_module

    messages = [{"role": "user", "content": "test"}]
    with patch("app.core.token_budget.settings") as mock_settings:
        mock_settings.openai_model = "gpt-4o-mini"
        tb_module._encoding = None
        tb_module._encoding_model = None
        result1 = estimate_tokens(messages)
        enc_model_1 = tb_module._encoding_model

    with patch("app.core.token_budget.settings") as mock_settings:
        mock_settings.openai_model = "gpt-4o"
        result2 = estimate_tokens(messages)
        enc_model_2 = tb_module._encoding_model

    assert result1 > 0
    assert result2 > 0
    assert enc_model_1 == "gpt-4o-mini"
    assert enc_model_2 == "gpt-4o"
    assert enc_model_1 != enc_model_2
