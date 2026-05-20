# TFT-gogo 🎮

> 롤토체스(TFT) 유저를 위한 전적 분석 · AI 메타 추천 · 커뮤니티 통합 플랫폼

<br>

## 소개

**TFT-gogo**는 롤토체스 유저가 자신의 전적을 분석하고, AI 기반 덱 추천을 받으며, 다른 유저와 전략을 공유할 수 있는 통합 서비스입니다.

| 기능 | 설명 |
|------|------|
| 🔍 전적 검색 | 소환사명으로 최근 매치 기록, 증강, 덱 구성 조회 |
| 🤖 AI 덱 추천 | 현재 메타 기반 최적 덱 조합을 AI가 추천 |
| 💬 커뮤니티 | 덱 공유, 공략 게시판, 댓글 기능 |
| 📖 게임 가이드 | 챔피언 · 시너지 · 증강 정보 정리 |

<br>

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Backend** | Spring Boot 3, MySQL, Redis, JWT |
| **AI Server** | FastAPI, PostgreSQL, pgvector, OpenAI API |
| **Frontend** | React 18 (Vite), Zustand, TanStack Query |
| **Infra** | AWS EC2, Nginx, Docker, GitHub Actions |

<br>

## 프로젝트 구조

```
TFT-gogo/
├── backend/        # Spring Boot API 서버
├── ai-server/      # FastAPI AI 추천 서버
├── frontend/       # React 클라이언트
└── docker-compose.yml
```
