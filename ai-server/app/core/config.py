from pydantic import field_validator, model_validator
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_env: str = "development"
    app_port: int = 8000
    secret_key: str = "dev-secret"

    database_url: str = "postgresql+asyncpg://user:password@localhost:5432/tftgogo_ai"

    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    openai_timeout: int = 30

    chat_max_input_tokens: int = 4000
    recommend_max_input_tokens: int = 6000

    rate_limit_requests: int = 30
    rate_limit_window: int = 60

    circuit_breaker_threshold: int = 5
    circuit_breaker_window: int = 60
    circuit_breaker_cooldown: int = 30

    internal_secret: str

    riot_api_key: str = ""
    cors_allowed_origins: str = (
        "http://localhost:8080,"
        "http://localhost:5173"
    )

    @field_validator("internal_secret")
    @classmethod
    def internal_secret_must_be_set(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("INTERNAL_SECRET 환경변수가 설정되지 않았습니다.")
        return v

    @model_validator(mode="after")
    def production_openai_api_key_must_be_set(self):
        if self.app_env.strip().lower() == "production" and not self.openai_api_key.strip():
            raise ValueError("OPENAI_API_KEY must be set when APP_ENV=production.")
        return self

    @property
    def cors_allowed_origin_list(self) -> list[str]:
        return [
            origin.strip()
            for origin in self.cors_allowed_origins.split(",")
            if origin.strip()
        ]

    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
