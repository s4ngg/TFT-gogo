<spec domain="ai-recommend">

<purpose>
소환사 전적 데이터를 분석해 현재 메타에 맞는 덱을 AI가 추천하는 기능.
Page: AiRecommend (/ai-recommend)
Backend: Spring Boot + ai-server (FastAPI)
</purpose>

<routes>
- /ai-recommend → AI 분석 결과 + 추천 덱 페이지
</routes>

<api>
<endpoints>
- POST /api/analyze
    요청: AnalyzeRequest (summoner_name, tag_line, matches)
    응답: AnalyzeResponse (stats, good_traits, bad_traits, augments, deck_reasons)

- POST /api/analyze/with-meta
    요청: AnalyzeRequest + MetaDeck 목록
    응답: AnalyzeResponse (메타 매칭 + OpenAI 추천 이유 포함)
</endpoints>

<flow>
1. 소환사 검색 후 전적 연동 (Spring 백엔드 → Riot API)
2. Spring 백엔드가 전적 데이터 + 현재 메타 덱을 AI 서버로 전달
3. AI 서버가 시너지별 승률 집계 + OpenAI 추천 이유 생성
4. 프론트 AiRecommend 페이지에 결과 표시
5. 소환사 미연동 시 ConnectPrompt 화면 표시
</flow>

<frontend-never-calls-ai-server-directly>
프론트는 ai-server를 직접 호출하지 않는다. 반드시 Spring 백엔드를 통해 프록시한다.
</frontend-never-calls-ai-server-directly>
</api>

<analysis-features>
- 최근 N게임 요약 (평균 등수, TOP4율, 1등 비율)
- 시너지별 승률 성적 (잘 맞는 / 잘 안 맞는 시너지)
- 현재 메타 덱과 내 플레이 스타일 매칭
- OpenAI를 이용한 추천 이유 자연어 생성
</analysis-features>

<business-rules>
- 증강 추천은 포함하지 않는다 (Riot API가 증강 데이터를 제공하지 않음)
- 소환사 미연동 시 목데이터(mockAiRecommendation)로 fallback
- OpenAI API 키 미설정 시 기본 추천 이유로 fallback (서버 오류 없음)
- ai-server 오류(타임아웃, 파싱 실패)는 UI에서 명확한 에러 상태로 표시
- 시너지 집계는 활성화된 시너지(style > 0)만 대상으로 한다
- 최소 3게임 이상 플레이한 시너지만 통계에 포함
</business-rules>

<frontend-structure>
- frontend/src/pages/AiRecommend/
  - AiRecommend.tsx              — 페이지 조합
  - components/ConnectPrompt.tsx — 소환사 미연동 안내
  - components/StatCard.tsx      — 통계 카드
  - components/TraitPerformanceList.tsx — 시너지 성적 목록
  - components/DeckPerformance.tsx      — 시너지 성적 패널
  - components/AugmentAnalysis.tsx      — 증강 분석 패널
  - components/AiRecommendedDecks.tsx   — AI 추천 덱 목록
</frontend-structure>

<ai-server-structure>
- ai-server/app/
  - api/analyze.py       — 엔드포인트 정의
  - services/analyzer.py    — 통계 집계 로직
  - services/recommender.py — 메타 매칭 + OpenAI 호출
  - models/match.py      — 요청/응답 스키마
</ai-server-structure>

</spec>
