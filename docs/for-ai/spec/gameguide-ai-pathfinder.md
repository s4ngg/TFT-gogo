<spec domain="gameguide-ai-pathfinder">

<purpose>
Guide 페이지의 챔피언, 시너지, 아이템, 증강체 데이터를 사용자가 직접 선택하면
AI가 해당 조합을 학습/운영 루트로 재구성해 주는 기능.
Page: Guide (/guide)
Backend: Spring Boot 프록시 + ai-server(FastAPI) + OpenAI
</purpose>

<positioning>
- 기존 AI Recommend는 소환사 전적과 메타 덱을 바탕으로 개인화 덱을 추천한다.
- 기존 AI Chat은 소환사 상세 페이지에서 최근 전적 컨텍스트로 자유 질문에 답한다.
- GameGuide AI Pathfinder는 전적을 사용하지 않고, Guide 정적 데이터만 바탕으로 "이 정보를 어떻게 이해하고 연결할지"를 안내한다.
- 메타 덱 추천, 승률 예측, 소환사 플레이 분석은 이 기능의 책임이 아니다.
</positioning>

<routes>
- /guide -> 기존 Guide 페이지 안에 AI 패스파인더 패널 또는 하단 도킹 패널로 제공
</routes>

<user-flow>
1. 사용자가 Guide 페이지에서 챔피언/시너지/아이템/증강체 카드를 탐색한다.
2. 프론트 1차 MVP에서는 우측 하단 챗봇 위젯에 질문을 직접 입력한다.
3. 카드 연동 단계에서는 각 카드의 "AI에게 물어보기" 아이콘 버튼으로 해당 항목 1개를 선택하고 챗봇을 연다.
4. 사용자는 별도 모드 선택 없이 선택된 카드 기준으로 질문을 입력한다.
   AI는 질문 의도에 따라 개념 설명, 운영 루트, 시너지 확장, 아이템 활용, 전환 후보를 통합 판단한다.
5. 프론트가 인증된 사용자 세션으로 Spring 백엔드 POST /api/ai/gameguide-pathfinder를 호출한다.
6. Spring은 사용자별 AI 요청 제한을 확인하고, 선택된 Guide ref가 있으면 같은 패치의 저장 데이터로 재조회해 AI 서버에 전달할 컨텍스트를 만든다.
7. AI 서버가 구조화된 JSON 응답을 생성한다.
8. Spring은 응답의 추천 Guide ref/source ref를 검증한 뒤 ApiResponse로 래핑해 프론트에 반환한다.
9. 프론트는 우측 하단 챗봇 위젯 안에서 요약, 운영 단계, 주의점을 메시지형 카드로 표시한다.
</user-flow>

<api>
<frontend-to-spring>
<endpoint>POST /api/ai/gameguide-pathfinder</endpoint>

<request-body>
{
  "patchVersion": "17.3",
  "activeTab": "traits" | "items" | "augments" | "champions",
  "mode": "AUTO",
  "selectedRefs": [
    {
      "guideType": "TRAIT" | "ITEM" | "AUGMENT" | "CHAMPION",
      "targetKey": "string",
      "name": "string"
    }
  ],
  "candidateRefs": [
    {
      "guideType": "TRAIT" | "ITEM" | "AUGMENT" | "CHAMPION",
      "targetKey": "string",
      "name": "string"
    }
  ],
  "conversationHistory": [
    {
      "role": "user" | "assistant",
      "content": "string"
    }
  ],
  "question": "string"
}
</request-body>

<request-rules>
- patchVersion은 현재 Guide 화면의 기준 패치를 보낸다.
- 프론트 1차 MVP는 question-only 챗봇이므로 selectedRefs는 0개 이상 5개 이하를 허용한다.
- 카드 연동 단계에서는 카드 버튼 클릭 시 selectedRefs에 해당 카드 1개를 담고, 챗봇을 우측 하단에서 연다.
- 사용자가 챗봇을 직접 열면 selectedRefs 없이 activeTab + candidateRefs 기반으로 질문한다.
- selectedRefs의 중복 guideType + targetKey 조합은 Spring에서 제거한다.
- question은 프론트 1차 MVP에서 필수이며 최대 500자까지 허용한다.
- conversationHistory는 최근 6개 메시지까지만 보낸다.
- conversationHistory는 이어묻기 의도 파악에만 사용하고 새로운 Guide 데이터 근거로 취급하지 않는다.
- activeTab은 질문만 있는 경우 AI가 현재 Guide 문맥을 좁히는 힌트로 사용한다.
- mode는 프론트에서 사용자에게 노출하지 않고 AUTO로 고정한다.
- 프론트는 AI 서버를 직접 호출하지 않는다.
- question-only chat sends up to 20 currently visible activeTab Guide entries through candidateRefs.
- candidateRefs are not user-selected refs; they are only an allow-list for AI recommended links.
- POST /api/ai/gameguide-pathfinder는 인증 필요 API이며 사용자별 AI rate limit을 통과해야 한다.
</request-rules>

<response-body>
ApiResponse&lt;GameGuideAiPathfinderResponse&gt;
</response-body>
</frontend-to-spring>

<spring-to-ai>
<endpoint>POST http://ai-server:8000/api/gameguide/pathfinder</endpoint>

<request-body>
{
  "patch_version": "17.3",
  "active_tab": "traits",
  "mode": "AUTO",
  "selected_entries": [
    {
      "guide_type": "TRAIT",
      "target_key": "TFT17_ExampleTrait",
      "name": "string",
      "summary": "string",
      "data": {}
    }
  ],
  "candidate_refs": [
    {
      "guide_type": "CHAMPION",
      "target_key": "TFT17_ExampleChampion",
      "name": "string",
      "reason_hint": "selected trait includes this champion"
    }
  ],
  "conversation_history": [
    {
      "role": "user|assistant",
      "content": "string"
    }
  ],
  "question": "string"
}
</request-body>

<notes>
- selected_entries는 Spring이 Guide split table에서 재조회한 데이터만 사용한다.
- candidate_refs는 AI가 추천 링크를 만들 때 고를 수 있는 후보 목록이다.
- candidate_refs는 같은 patchVersion의 실제 Guide 항목만 포함한다.
- conversation_history는 현재 질문이 이전 답변에 이어지는지 해석하는 보조 문맥이다.
- conversation_history에만 있는 내용은 evidence_notes 근거로 쓰지 않는다.
- 전체 Guide catalog를 프롬프트에 넣지 않고, 선택 항목과 관련 후보만 전달한다.
</notes>
</spring-to-ai>
</api>

<response-contract>
<gameguide-ai-pathfinder-response>
{
  "title": "string",
  "summary": "string",
  "coreConcepts": ["string"],
  "evidenceNotes": ["string"],
  "creativeSuggestions": ["string"],
  "phasePlan": [
    {
      "phase": "EARLY" | "MID" | "LATE" | "ANY",
      "title": "string",
      "description": "string",
      "guideRefs": [
        {
          "guideType": "TRAIT" | "ITEM" | "AUGMENT" | "CHAMPION",
          "targetKey": "string",
          "name": "string"
        }
      ]
    }
  ],
  "recommendedRefs": [
    {
      "guideType": "TRAIT" | "ITEM" | "AUGMENT" | "CHAMPION",
      "targetKey": "string",
      "name": "string",
      "reason": "string"
    }
  ],
  "avoidMistakes": ["string"],
  "sourceRefs": [
    {
      "guideType": "TRAIT" | "ITEM" | "AUGMENT" | "CHAMPION",
      "targetKey": "string",
      "name": "string"
    }
  ],
  "limitations": ["string"],
  "isFallback": false
}
</gameguide-ai-pathfinder-response>

<response-rules>
- title은 한 줄 카드 제목으로 표시 가능해야 한다.
- summary는 2문장 이내로 제한한다.
- evidenceNotes는 제공된 Guide 데이터에서 확인 가능한 근거만 담는다.
- creativeSuggestions는 일반 TFT 운영 감각에 기반한 가능한 선택지를 담고, 확정 표현을 피한다.
- phasePlan은 최대 4개 항목만 반환한다.
- recommendedRefs는 최대 5개 항목만 반환한다.
- avoidMistakes는 최대 4개 항목만 반환한다.
- sourceRefs는 실제 AI 판단에 사용한 선택 Guide 항목을 표시한다.
- Spring은 recommendedRefs, guideRefs, sourceRefs가 실제 Guide 항목인지 검증하고, 검증 실패 항목은 제거한다.
- isFallback=true인 응답은 OpenAI 미설정, AI 서버 장애, JSON 파싱 실패 시 사용한다.
</response-rules>
</response-contract>

<business-rules>
- AI는 선택된 Guide 데이터와 candidate_refs에 없는 챔피언/시너지/아이템/증강체를 추천 링크로 만들 수 없다.
- 현재 Guide 데이터에 없는 승률, 평균 등수, 픽률, TOP4율을 생성하거나 추측하지 않는다.
- Guide metric refresh가 구현되기 전까지 통계 기반 추천 문구를 사용하지 않는다.
- "현재 메타 1티어", "승률이 높다" 같은 판단은 별도 메타 덱/통계 근거가 없으면 금지한다.
- 소환사명, tagLine, 최근 전적, 개인 플레이 스타일은 요청/응답에 포함하지 않는다.
- 사용자의 question은 선택된 Guide 항목을 해석하는 보조 질문으로만 사용한다.
- AI 응답은 한국어만 사용한다.
- AI 응답은 JSON만 반환하도록 프롬프트를 제한하고, ai-server에서 스키마 검증 후 반환한다.
- 가이드 데이터 기반 판단은 evidenceNotes, AI의 보조 운영 제안은 creativeSuggestions로 분리한다.
- creativeSuggestions가 포함되면 limitations에 확정 데이터가 아니라는 한계를 짧게 남긴다.
- Spring은 AI 장애를 프론트 에러로 직접 노출하지 않고 fallback 응답을 반환한다.
- AI 서버는 OpenAI 호출 전에 전역 rate limit을 적용하고, 응답은 BaseResponse envelope의 data에 GameGuidePathfinderResponse를 담아 반환한다.
</business-rules>

<fallback-behavior>
- OpenAI API 키가 없거나 OpenAI 응답 파싱에 실패하면 AI 서버가 deterministic fallback을 반환한다.
- AI 서버 호출 자체가 실패하거나 응답 payload가 비어 있으면 Spring이 deterministic fallback을 반환한다.
- fallback summary는 "현재 가이드 화면 기준의 기본 안내를 표시합니다." 수준으로 제한한다.
- fallback은 phasePlan을 최소화하고 recommendedRefs는 비워 둔다.
- fallback에서도 존재하지 않는 메트릭이나 메타 판단을 만들지 않는다.
</fallback-behavior>

<frontend-auth-ux>
- GameGuide AI is an authenticated feature.
- When the user is not logged in, clicking a Guide card AI button must open the widget with the selected ref context
  but must not render an extra login CTA/button inside the widget.
- The logged-out widget empty state text is exactly: `로그인이 필요합니다`.
- The logged-out input placeholder is exactly: `로그인이 필요합니다`.
- The widget must not include internal login links or login buttons. The global TopBar login action remains owned by
  the shared layout and is not part of the GameGuide AI widget contract.
- The frontend must preserve explicit AUTH_REQUIRED and RATE_LIMITED messages from the API layer for mutation errors.
- Mobile QA must verify the opened widget and Guide card AI buttons do not create horizontal overflow at 390px width.
</frontend-auth-ux>

<latest-implementation-contract>
- Guide card buttons create a single selected ref through gameGuideAiRefs.ts and open the floating
  GameGuideAiChatWidget.
- Question-only requests may omit selected refs, but card-click requests send exactly the clicked card ref.
- Frontend never calls the FastAPI ai-server directly; it calls Spring POST /api/ai/gameguide-pathfinder.
- Spring validates selected/candidate refs against the Guide split tables for the requested patchVersion before
  forwarding compact selected_entries and candidate_refs to ai-server.
- ai-server returns BaseResponse with GameGuidePathfinderResponse. OpenAI failures, circuit-breaker open state,
  JSON parsing failures, and token-budget failures must produce deterministic fallback responses instead of exposing
  raw provider errors to the user.
- AI responses may include evidence_notes and creative_suggestions, but unavailable metrics such as win rate,
  pick rate, average placement, or TOP4 rate must not be invented until the guide metric refresh contract exists.
</latest-implementation-contract>

<frontend-structure>
- frontend/src/api/gameGuideAiPathfinderApi.ts
  - request/response 타입
  - POST /api/ai/gameguide-pathfinder 호출
- frontend/src/pages/Guide/hooks/useGameGuideAiPathfinder.ts
  - TanStack Query mutation
  - 선택 ref, mode, loading/error/fallback 상태 관리
- frontend/src/pages/Guide/components/GameGuideAiChatWidget.tsx
  - 우측 하단 floating 챗봇 위젯
  - question-only MVP 입력창과 메시지 목록
  - 선택 카드 칩, phasePlan, recommendedRefs, avoidMistakes, sourceRefs 표시
- frontend/src/pages/Guide/components/*GuideView.tsx
  - 각 카드에 "AI에게 물어보기" 액션 연결
- frontend/src/pages/Guide/utils/gameGuideAiRefs.ts
  - 카드 guideType/name/targetKey를 GameGuideAiPathfinderRef로 변환
</frontend-structure>

<backend-structure>
- backend/.../domain/ai/controller/GameGuideAiPathfinderController.java
  - POST /api/ai/gameguide-pathfinder
- backend/.../domain/ai/controller/docs/GameGuideAiPathfinderControllerDocs.java
  - Swagger 문서
- backend/.../domain/ai/dto/GameGuideAiPathfinderRequest.java
  - 프론트 요청 DTO
- backend/.../domain/ai/dto/GameGuideAiPathfinderResponse.java
  - 프론트 응답 DTO
- backend/.../domain/ai/service/GameGuideAiPathfinderService.java
  - Guide ref 검증, Guide 데이터 재조회, candidate_refs 구성, AI 서버 호출
- backend/.../domain/ai/client/AiServerClient.java
  - POST /api/gameguide/pathfinder 호출 메서드 추가
- backend/.../domain/guide/service/GuideService.java
  - 필요한 경우 guideType + targetKey + patchVersion 단건 조회 메서드 추가
</backend-structure>

<ai-server-structure>
- ai-server/app/api/gameguide_pathfinder.py
  - POST /api/gameguide/pathfinder
- ai-server/app/models/gameguide_pathfinder.py
  - GameGuidePathfinderRequest, GameGuidePathfinderResponse Pydantic 모델
- ai-server/app/models/common.py
  - BaseResponse envelope 모델
- ai-server/app/services/gameguide_pathfinder.py
  - 프롬프트 구성, OpenAI 호출, JSON 파싱, fallback 생성
- ai-server/tests/test_gameguide_pathfinder.py
  - 스키마 검증, 프롬프트 제한, fallback, JSON 파싱 실패 테스트
</ai-server-structure>

<data-context>
<selected-entry>
- guide_type
- target_key
- name
- summary
- data
</selected-entry>

<candidate-ref-generation>
- 선택된 TRAIT:
  - champions 배열에 포함된 챔피언을 candidate_refs로 추가한다.
  - specialUnits는 실제 champion Guide 항목이 아니면 candidate_refs에 넣지 않는다.
- 선택된 CHAMPION:
  - traits 배열에 포함된 시너지를 같은 patchVersion의 trait Guide에서 찾아 candidate_refs로 추가한다.
  - bestItems가 비어 있으면 아이템 후보를 만들지 않는다.
- 선택된 ITEM:
  - bestUsers가 비어 있으면 챔피언 후보를 만들지 않는다.
  - combinations의 component item은 현재 Guide item row가 없을 수 있으므로 링크 후보로 강제하지 않는다.
- 선택된 AUGMENT:
  - tags가 동일한 augment를 후보로 추가할 수 있으나, 동일 태그만으로 강한 추천 문구를 만들지 않는다.
</candidate-ref-generation>
</data-context>

<validation>
<frontend>
- question-only 요청 payload 생성 테스트
- 카드 버튼 클릭 시 챗봇 오픈 및 selectedRefs 1개 전달 테스트
- 선택 ref 0~5개 제한 테스트
- 선택 ref 중복 제거 테스트
- mode=AUTO 요청 payload 생성 테스트
- fallback 응답 UI 표시 테스트
- 브라우저 수동 검증: 카드 AI 버튼 렌더링, 선택 카드 칩 표시, 질문 전송 fallback 확인
</frontend>

<frontend-latest-validation>
- Logged-out widget QA: clicking a Guide card AI button opens the widget, shows exactly `로그인이 필요합니다`,
  and renders no login CTA/link/button inside the GameGuide AI widget.
- Mobile browser QA: /guide at 390px width keeps body/document scrollWidth equal to viewport width after opening
  the GameGuide AI widget.
</frontend-latest-validation>

<backend>
- selectedRefs empty 또는 5개 초과 시 INVALID_INPUT
- 존재하지 않는 guideType/targetKey/patchVersion 조합은 INVALID_INPUT
- Spring이 클라이언트 dataJson을 신뢰하지 않고 DB 데이터를 재조회하는지 테스트
- 인증 userId가 없거나 사용자별 AI rate limit을 초과하면 AI 서버를 호출하지 않는지 테스트
- AI 서버 장애 시 ApiResponse&lt;GameGuideAiPathfinderResponse&gt; fallback 반환 테스트
- AI 응답의 recommendedRefs/sourceRefs 검증 및 미존재 ref 제거 테스트
</backend>

<ai-server>
- OpenAI 미설정 시 fallback 반환
- OpenAI 응답이 JSON이 아니면 fallback 반환
- 없는 메트릭을 만들지 않도록 시스템 프롬프트에 제한 문구가 포함되는지 테스트
- selected_entries와 candidate_refs가 토큰 예산 내로 제한되는지 테스트
- /api/gameguide/pathfinder가 전역 rate limit 대상인지 테스트
- API 응답이 BaseResponse envelope로 반환되는지 테스트
</ai-server>
</validation>

<open-questions>
- 선택 ref를 1개 카드 단위로 유지할지, 이후 여러 카드를 비교 선택하는 기능으로 확장할지 결정이 필요하다.
- 추천 ref 클릭 시 검색 이동만 할지, 상세 모달/카드 강조까지 연결할지 후속 UX 결정이 필요하다.
</open-questions>

</spec>
