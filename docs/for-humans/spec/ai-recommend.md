# AiRecommend — AI 덱 추천

## 개요

현재 보유 유닛/아이템을 입력하면 AI가 최적 덱을 추천해주는 기능.

---

## 기능 목록

- AI가 현재 보유 유닛/아이템을 분석해 최적 덱을 추천한다
- 추천 덱은 유닛 구성, 아이템 배치, 포지셔닝을 포함한다

---

## 기술 구조

- ai-server (FastAPI) RAG 파이프라인: pgvector + OpenAI Embeddings API + LangChain
- 프론트엔드 → 백엔드 프록시 → ai-server 순서로 요청이 전달된다
