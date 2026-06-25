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

def test_INTERNAL_SECRET_미설정_시_Settings_초기화_실패():
    # given: INTERNAL_SECRET 환경변수 없음
    from pydantic_settings import BaseSettings
    from pydantic import field_validator

    class TestSettings(BaseSettings):
        internal_secret: str

        @field_validator("internal_secret")
        @classmethod
        def internal_secret_must_be_set(cls, v: str) -> str:
            if not v or not v.strip():
                raise ValueError("INTERNAL_SECRET 환경변수가 설정되지 않았습니다.")
            return v

        class Config:
            env_file = None

    # when / then
    with pytest.raises(ValidationError):
        TestSettings(_env_file=None)


def test_INTERNAL_SECRET_빈값_시_Settings_초기화_실패():
    from pydantic_settings import BaseSettings
    from pydantic import field_validator

    class TestSettings(BaseSettings):
        internal_secret: str

        @field_validator("internal_secret")
        @classmethod
        def internal_secret_must_be_set(cls, v: str) -> str:
            if not v or not v.strip():
                raise ValueError("INTERNAL_SECRET 환경변수가 설정되지 않았습니다.")
            return v

        class Config:
            env_file = None

    with pytest.raises(ValidationError):
        TestSettings(internal_secret="")


def test_INTERNAL_SECRET_공백값_시_Settings_초기화_실패():
    from pydantic_settings import BaseSettings
    from pydantic import field_validator

    class TestSettings(BaseSettings):
        internal_secret: str

        @field_validator("internal_secret")
        @classmethod
        def internal_secret_must_be_set(cls, v: str) -> str:
            if not v or not v.strip():
                raise ValueError("INTERNAL_SECRET 환경변수가 설정되지 않았습니다.")
            return v

        class Config:
            env_file = None

    with pytest.raises(ValidationError):
        TestSettings(internal_secret="   ")


# ── 엔드포인트 인증 테스트 ────────────────────────────────────────────────

TEST_SECRET = "test-secret-value"

CHAT_PAYLOAD = {
    "messages": [{"role": "user", "content": "안녕"}],
    "context": None,
}


@pytest.fixture
def client():
    with patch("app.core.config.settings") as mock_settings:
        mock_settings.internal_secret = TEST_SECRET
        mock_settings.app_env = "test"
        mock_settings.cors_allowed_origin_list = []
        from app.main import app
        yield TestClient(app, raise_server_exceptions=False)


def test_X_Internal_Secret_헤더_누락_시_403_반환(client):
    # given: 헤더 없음
    # when
    response = client.post("/api/chat", json=CHAT_PAYLOAD)
    # then
    assert response.status_code == 403


def test_X_Internal_Secret_불일치_시_403_반환(client):
    # given: 잘못된 시크릿
    # when
    response = client.post(
        "/api/chat",
        json=CHAT_PAYLOAD,
        headers={"X-Internal-Secret": "wrong-secret"},
    )
    # then
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_X_Internal_Secret_일치_시_정상_처리(client):
    # given: 올바른 시크릿 + chat 서비스 mock
    with patch("app.services.chat.chat", new_callable=AsyncMock) as mock_chat:
        from app.models.chat import ChatResponse
        mock_chat.return_value = ChatResponse(reply="테스트 응답")

        # when
        response = client.post(
            "/api/chat",
            json=CHAT_PAYLOAD,
            headers={"X-Internal-Secret": TEST_SECRET},
        )
        # then
        assert response.status_code == 200
