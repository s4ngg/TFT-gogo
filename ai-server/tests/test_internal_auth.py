"""
내부 인증(X-Internal-Secret) 계약 테스트

- Settings 미설정/빈값/공백값 시작 실패 검증
- /api/chat 엔드포인트 헤더 누락·불일치·정상 경로 검증
"""
import pytest
from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient
from pydantic import ValidationError


# ── Settings 시작 실패 테스트 ────────────────────────────────────────────

def test_INTERNAL_SECRET_미설정_시_Settings_초기화_실패(monkeypatch):
    monkeypatch.delenv("INTERNAL_SECRET", raising=False)

    from app.core.config import Settings
    with pytest.raises(ValidationError):
        Settings(_env_file=None)


def test_INTERNAL_SECRET_빈값_시_Settings_초기화_실패(monkeypatch):
    monkeypatch.delenv("INTERNAL_SECRET", raising=False)

    from app.core.config import Settings
    with pytest.raises(ValidationError):
        Settings(_env_file=None, internal_secret="")


def test_INTERNAL_SECRET_공백값_시_Settings_초기화_실패(monkeypatch):
    monkeypatch.delenv("INTERNAL_SECRET", raising=False)

    from app.core.config import Settings
    with pytest.raises(ValidationError):
        Settings(_env_file=None, internal_secret="   ")


# ── 엔드포인트 인증 테스트 ────────────────────────────────────────────────

TEST_SECRET = "test-secret-value"


def test_APP_ENV_production_OPENAI_API_KEY_missing_Settings_초기화_실패(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)

    from app.core.config import Settings
    with pytest.raises(ValidationError) as exc_info:
        Settings(
            _env_file=None,
            app_env="production",
            internal_secret=TEST_SECRET,
            openai_api_key="",
        )

    assert "OPENAI_API_KEY" in str(exc_info.value)


def test_APP_ENV_development_OPENAI_API_KEY_missing_Settings_초기화_허용(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)

    from app.core.config import Settings
    settings = Settings(
        _env_file=None,
        app_env="development",
        internal_secret=TEST_SECRET,
        openai_api_key="",
    )

    assert settings.openai_api_key == ""


CHAT_PAYLOAD = {
    "messages": [{"role": "user", "content": "안녕"}],
    "context": None,
}


@pytest.fixture
def client():
    with patch("app.core.config.settings") as mock_settings, \
         patch("app.core.security.settings") as mock_security_settings:
        mock_settings.internal_secret = TEST_SECRET
        mock_settings.app_env = "test"
        mock_settings.cors_allowed_origin_list = []
        mock_security_settings.internal_secret = TEST_SECRET
        from app.main import app
        yield TestClient(app, raise_server_exceptions=False)


def test_X_Internal_Secret_헤더_누락_시_403_반환(client):
    response = client.post("/api/chat", json=CHAT_PAYLOAD)
    assert response.status_code == 403


def test_X_Internal_Secret_불일치_시_403_반환(client):
    response = client.post(
        "/api/chat",
        json=CHAT_PAYLOAD,
        headers={"X-Internal-Secret": "wrong-secret"},
    )
    assert response.status_code == 403


def test_X_Internal_Secret_일치_시_정상_처리(client):
    with patch("app.services.chat.chat", new_callable=AsyncMock) as mock_chat:
        mock_chat.return_value = "테스트 응답"

        response = client.post(
            "/api/chat",
            json=CHAT_PAYLOAD,
            headers={"X-Internal-Secret": TEST_SECRET},
        )
        assert response.status_code == 200
