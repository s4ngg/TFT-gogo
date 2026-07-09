FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV APP_ENV=production

EXPOSE 8000

# 벡터 검색(pgvector)은 추천 로직의 보강 신호일 뿐 필수 경로가 아니다
# (DB/OpenAI 실패 시 기존 set-overlap 방식으로 자동 폴백, app/services/embedding.py 참고).
# 그래서 마이그레이션 실패가 서버 기동 자체를 막지 않도록 && 대신 실패를 흡수하고 넘어간다.
CMD ["sh", "-c", "alembic upgrade head || echo '[startup] alembic migration failed or DB unavailable — continuing without it, vector search will fall back to set-overlap only' >&2; exec uvicorn app.main:app --host 0.0.0.0 --port 8000"]
