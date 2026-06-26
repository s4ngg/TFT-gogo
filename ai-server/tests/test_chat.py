"""
chat.py (AI 채팅 서비스) 단위 테스트

given / when / then 패턴으로 작성.
OpenAI 호출은 Mock 처리.
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from openai import APIStatusError, APITimeoutError

from app.models.chat import ChatContext, ChatMessage, ChatRequest
from app.services.chat import chat, _build_context_block, _FALLBACK_REPLY

_BUDGET_PATH = "app.services.chat.check_budget"
_BREAKER_PATH = "app.services.chat.openai_breaker"


# ── 픽스처 ──────────────────────────────────────────────────────────────

def _make_request(content: str = "어떤 덱 추천해요?", context: ChatContext | None = None) -> ChatRequest:
    return ChatRequest(
        messages=[ChatMessage(role="user", content=content)],
        context=context,
    )


def _make_context(
    summoner_name: str = "테스트소환사",
    tag_line: str = "KR1",
    stats_summary: str | None = None,
    good_traits: list[str] | None = None,
    bad_traits: list[str] | None = None,
) -> ChatContext:
    return ChatContext(
        summoner_name=summoner_name,
        tag_line=tag_line,
        stats_summary=stats_summary,
        good_traits=good_traits,
        bad_traits=bad_traits,
    )


def _mock_openai_response(content: str) -> MagicMock:
    choice = MagicMock()
    choice.message.content = content
    response = MagicMock()
    response.choices = [choice]
    return response


# ── 테스트 케이스 ────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_API키_미설정_시_fallback_응답_반환():
    # given
    request = _make_request()

    # when
    with patch("app.services.chat.settings") as mock_settings:
        mock_settings.openai_api_key = ""
        result = await chat(request)

    # then
    assert result == _FALLBACK_REPLY


@pytest.mark.asyncio
async def test_OpenAI_정상_응답_시_reply_반환():
    # given
    request = _make_request()
    mock_client = AsyncMock()
    mock_client.chat.completions.create.return_value = _mock_openai_response("신궁 덱을 추천드립니다.")

    # when
    with patch("app.services.chat.settings") as mock_settings, \
         patch("app.services.chat._get_client", return_value=mock_client), \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "sk-test"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.chat_max_input_tokens = 4000
        mock_breaker.is_open.return_value = False
        result = await chat(request)

    # then
    assert result == "신궁 덱을 추천드립니다."


@pytest.mark.asyncio
async def test_OpenAI_타임아웃_시_fallback_응답_반환():
    # given
    request = _make_request()
    mock_client = AsyncMock()
    mock_client.chat.completions.create.side_effect = APITimeoutError(request=MagicMock())

    # when
    with patch("app.services.chat.settings") as mock_settings, \
         patch("app.services.chat._get_client", return_value=mock_client), \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "sk-test"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.chat_max_input_tokens = 4000
        mock_breaker.is_open.return_value = False
        result = await chat(request)

    # then
    assert result == _FALLBACK_REPLY


@pytest.mark.asyncio
async def test_OpenAI_API오류_시_fallback_응답_반환():
    # given
    request = _make_request()
    mock_response = MagicMock()
    mock_response.status_code = 500
    mock_client = AsyncMock()
    mock_client.chat.completions.create.side_effect = APIStatusError(
        "Internal Server Error", response=mock_response, body=None
    )

    # when
    with patch("app.services.chat.settings") as mock_settings, \
         patch("app.services.chat._get_client", return_value=mock_client), \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "sk-test"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.chat_max_input_tokens = 4000
        mock_breaker.is_open.return_value = False
        result = await chat(request)

    # then
    assert result == _FALLBACK_REPLY


def test_컨텍스트_포함_시_context_block에_소환사_정보_포함():
    # given
    context = _make_context(
        summoner_name="테스트소환사",
        tag_line="KR1",
        stats_summary="평균 등수 4.2",
        good_traits=["신궁", "마법사"],
        bad_traits=["암살자"],
    )

    # when
    result = _build_context_block(context)

    # then
    assert "테스트소환사#KR1" in result
    assert "평균 등수 4.2" in result
    assert "신궁" in result
    assert "암살자" in result
