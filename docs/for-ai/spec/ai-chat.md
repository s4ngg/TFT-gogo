<spec domain="ai-chat">

<purpose>
소환사 전적 데이터를 컨텍스트로 활용해 TFT 관련 질문에 AI가 답변하는 채팅 기능.
Page: SearchDetail 우측 패널 (AiChat 컴포넌트)
Backend: Spring Boot (프록시) + ai-server (FastAPI) + OpenAI GPT-4o-mini
</purpose>

<routes>
- /summoner/:gameName/:tagLine → 소환사 상세 페이지 내 AI 채팅 패널
</routes>

<api>
<endpoints>
- POST /api/ai/chat
    요청: AiChatRequest (messages, context)
    응답: ApiResponse&lt;AiChatResponse&gt; { reply }
</endpoints>

<flow>
1. 소환사 상세 페이지 진입 시 초기 20게임 기반 chatContext 자동 구성
2. 사용자가 질문 입력 → 프론트가 Spring 백엔드로 POST /api/ai/chat
3. Spring이 AI 서버로 POST /api/chat 프록시
4. AI 서버가 시스템 프롬프트 + 사용자 컨텍스트 + 대화 이력을 OpenAI에 전달
5. AI 응답을 프론트에 반환
</flow>

<frontend-never-calls-ai-server-directly>
프론트는 ai-server를 직접 호출하지 않는다. 반드시 Spring 백엔드를 통해 프록시한다.
</frontend-never-calls-ai-server-directly>
</api>

<context-data>
프론트에서 초기 로드된 20게임(pages[0]) 기준으로 구성하며, "더보기" 눌러도 변하지 않는다.

포함 항목:
- summonerName, tagLine — 소환사 식별
- statsSummary — 최근 N게임, 평균 등수, TOP4율, 1등률
- goodTraits — 많이 사용한 시너지 상위 5개 (게임 수 + 평균 등수)
- recentMatches — 개별 매치 기록 (순위, 게임 타입, 덱 이름, 주요 챔피언 상위 4)
- topChampions — 많이 사용한 챔피언 상위 10개 (게임 수 + 평균 등수)

미포함 항목 (시스템 프롬프트에 한계로 명시):
- 아이템 빌드 정보
- 증강체 선택 정보
- LP 변동 및 승급/강등 이력
- 다른 소환사의 전적
- 전체 서버 메타 통계
</context-data>

<business-rules>
- 챔피언 이름은 CDragon locale(champByApiName)로 한국어 변환
- 컨텍스트 통계는 초기 20게임(pages[0])으로 고정 — rate limit 우회 목적
- AI 서버 미실행 또는 OpenAI 오류 시 fallback 메시지 반환 (200 응답)
- 대화 이력은 최대 20개 메시지로 제한
- 사용자 메시지는 최대 2000자, 응답은 max_tokens=600
- 시스템 프롬프트에 데이터 한계를 명시하여 AI가 없는 데이터를 추측하지 않도록 함
</business-rules>

<frontend-structure>
- frontend/src/api/aiChatApi.ts                — API 함수 + 타입 정의
- frontend/src/pages/AiRecommend/
  - components/AiChat.tsx                      — 채팅 UI 컴포넌트
  - hooks/useAiChat.ts                         — TanStack Query mutation 훅
- frontend/src/pages/SearchDetail/
  - SearchDetail.tsx                           — chatContext 구성 + AiChat 배치
</frontend-structure>

<backend-structure>
- backend/.../ai/controller/AiChatController.java      — POST /api/ai/chat
- backend/.../ai/controller/docs/AiChatControllerDocs.java — Swagger 문서
- backend/.../ai/dto/AiChatRequest.java                — 요청 DTO (messages + context)
- backend/.../ai/dto/AiChatResponse.java               — 응답 DTO (reply)
- backend/.../ai/service/AiChatService.java            — AI 서버 프록시 호출
- backend/.../ai/client/AiServerClient.java            — HTTP 클라이언트 (30s 타임아웃)
</backend-structure>

<ai-server-structure>
- ai-server/app/api/chat.py           — POST /api/chat 엔드포인트
- ai-server/app/services/chat.py      — 시스템 프롬프트 구성 + OpenAI 호출
- ai-server/app/models/chat.py        — Pydantic 요청/응답 스키마
- ai-server/tests/test_chat.py        — 채팅 서비스 단위 테스트
</ai-server-structure>

</spec>
