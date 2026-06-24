<spec domain="ai-server">

<stack>Python 3.12 · FastAPI · Pydantic v2 · SQLAlchemy (async) · pgvector · OpenAI API · Alembic · uvicorn</stack>

<conventions>

<structure>
- app/main.py         — FastAPI 앱 생성, 미들웨어, 라우터 등록만 담당
- app/api/            — 라우터와 엔드포인트 정의만 담당 (비즈니스 로직 금지)
- app/services/       — 비즈니스 로직, 분석 로직, OpenAI 호출
- app/models/         — Pydantic 요청/응답 스키마
- app/core/           — 환경변수(config.py), DB 연결, 의존성 주입
- 파일명: snake_case.py
- 클래스명: PascalCase
- 함수명: snake_case
</structure>

<response>
- 모든 응답은 Pydantic BaseModel 기반 스키마를 사용한다.
- 에러 응답은 FastAPI HTTPException을 사용하고 detail 필드에 메시지를 담는다.
- AI 서버 장애(타임아웃, OpenAI 실패 등)는 fallback 응답으로 처리한다. 500을 그대로 반환하지 않는다.
</response>

<models>
- 요청 모델: Request 접미사 사용 (예: AnalyzeRequest)
- 응답 모델: Response 접미사 사용 (예: AnalyzeResponse)
- 내부 집계용 모델: 접미사 없이 도메인명 사용 (예: TraitStat, MatchRecord)
- 모든 필드는 snake_case
</models>

<services>
- 서비스 파일 하나당 단일 책임 원칙 적용
  - analyzer.py   — 통계 집계만
  - recommender.py — 메타 매칭 + OpenAI 호출만
- OpenAI 클라이언트는 모듈 레벨 싱글턴으로 관리 (_get_client() 패턴)
- OpenAI API 키 미설정 시 반드시 fallback 동작 구현
- async def 함수에서 발생하는 예외는 반드시 try/except로 처리하고 logger로 기록
</services>

<logging>
- print() 사용 금지 → logging 모듈 사용
- 선언: logger = logging.getLogger(__name__)
- 레벨 구분:
  - logger.info    — 정상 흐름
  - logger.warning — 비정상이지만 fallback으로 복구 가능한 경우 (예: OpenAI 실패)
  - logger.error   — 복구 불가능한 오류
- 로그에 남기면 안 되는 것: API 키, JWT 토큰, 비밀번호
</logging>

<config>
- 환경변수는 pydantic-settings BaseSettings로 관리 (app/core/config.py)
- 하드코딩 금지 — 모든 외부 설정값은 settings 객체를 통해 참조
- .env 파일은 커밋 금지 (.env.example만 커밋)
</config>

<openai>
- 모델은 settings.openai_model로 관리 (기본값: gpt-4o-mini)
- 응답 파싱 실패 시 반드시 fallback 처리
- 비용 절감을 위해 max_tokens 명시 필수
</openai>

<external-api>
- Spring 백엔드 → AI 서버 통신: httpx AsyncClient 사용
- AI 서버 → Spring 호출 시 타임아웃 명시 (기본 10초)
- 외부 API 오류는 명확한 에러 응답으로 변환해 반환
</external-api>

<testing>
- 서비스 레이어만 단위 테스트
- pytest + pytest-asyncio 사용
- given/when/then 패턴
- 테스트 함수명은 한국어로 작성 (예: def test_시너지_통계_집계_정상동작():)
- OpenAI 호출은 Mock 처리
</testing>

</conventions>

</spec>
