from pydantic import field_validator
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_env: str = "development"
    app_port: int = 8000
    secret_key: str = "dev-secret"

    database_url: str = "postgresql+asyncpg://user:password@localhost:5432/tftgogo_ai"

    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"

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
