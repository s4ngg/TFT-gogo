<spec domain="ai-server-contract">

<purpose>
Spring 백엔드 ↔ AI 서버(FastAPI) 간 API 계약 명세.
팀원이 Spring 프록시 연동 작업 시 이 문서를 기준으로 한다.
</purpose>

<!-- ──────────────────────────────────────────────
     1. 전체 흐름
────────────────────────────────────────────── -->
<flow>
프론트 → Spring (GET /api/ai/recommend?gameName=&tagLine=)
           ↓
       Spring이 Riot API로 전적 20게임 수집
           ↓
       Spring이 AI 서버로 POST /api/analyze/with-meta 호출
       (전적 데이터 + 현재 메타 덱 목록 전달)
           ↓
       AI 서버 분석 결과 반환
           ↓
       Spring이 프론트에 그대로 응답
</flow>

<!-- ──────────────────────────────────────────────
     2. Spring → AI 서버 요청 스펙
────────────────────────────────────────────── -->
<spring-to-ai>

<endpoint>POST http://ai-server:8000/api/analyze/with-meta</endpoint>

<request-body>
{
  "summoner_name": "string",        // gameName (e.g. "Hide on bush")
  "tag_line": "string",             // tagLine (e.g. "KR1")
  "matches": [
    {
      "match_id": "string",         // e.g. "KR_7654321098"
      "game_datetime": 0,           // epoch milliseconds
      "game_length": 0.0,           // 게임 총 시간(초)
      "game_version": "string",     // 패치 버전 문자열
      "queue_type": "RANKED",       // "RANKED" | "NORMAL"
      "placement": 0,               // 최종 순위 1~8
      "level": 0,                   // 최종 소환사 레벨
      "last_round": 0,
      "gold_left": 0,
      "players_eliminated": 0,
      "total_damage_to_players": 0,
      "traits": [
        {
          "name": "string",         // e.g. "TFT17_Bruiser"
          "num_units": 0,
          "style": 0,               // 0=비활성, 1=브론즈, 2=실버, 3=골드, 4=프리즈매틱
          "tier_current": 0,
          "tier_total": 0
        }
      ],
      "units": [
        {
          "character_id": "string", // e.g. "TFT17_Jinx"
          "name": "string",
          "tier": 0,                // 성급 1~3
          "rarity": 0,              // 0=1코, 1=2코, 2=3코, 4=4코, 6=5코
          "item_names": ["string"]  // 장착 아이템 이름 목록
        }
      ]
    }
  ],
  "meta_decks": [                   // 현재 메타 덱 목록 (Spring이 DB에서 조회해 전달)
    {
      "rank": 0,
      "grade": "S",                 // "S" | "A" | "B" | "C" | "D"
      "trait_suffixes": ["string"], // 시너지 suffix 목록 (e.g. ["bruiser", "sniper"])
      "top4_rate": "string",        // e.g. "76.2%"
      "avg_place": "string",        // e.g. "3.14"
      "pick_rate": "string"         // e.g. "1.1%"
    }
  ]
}
</request-body>

<notes>
- matches는 최근 20게임 (랭크 게임만 필터링 권장, queue_type="RANKED")
- meta_decks는 Spring DB의 MetaDeck 테이블에서 현재 패치 기준으로 조회
- trait_suffixes: traitId에서 set 번호 제거한 소문자 suffix (e.g. "TFT17_Bruiser" → "bruiser")
</notes>

</spring-to-ai>

<!-- ──────────────────────────────────────────────
     3. AI 서버 → Spring 응답 스펙
────────────────────────────────────────────── -->
<ai-to-spring>

<response-body>
{
  "stats": {
    "recent_games": 0,
    "avg_place": "string",    // e.g. "3.25"
    "top4_rate": "string",    // e.g. "62.5%"
    "win_rate": "string"      // 1등 비율 e.g. "12.5%"
  },
  "good_traits": [
    {
      "name": "string",       // 시너지 suffix (e.g. "bruiser")
      "icon_url": "string",   // CDragon 아이콘 URL
      "tone": "string",       // "gold" | "silver" | "bronze"
      "count": 0,             // 활성화 단계 유닛 수
      "games": 0,
      "avg_place": "string",
      "top4_rate": "string"
    }
  ],
  "bad_traits": [ /* good_traits와 동일 구조 */ ],
  "augments": [],             // 현재는 빈 배열 (Riot API 증강 미제공)
  "deck_reasons": [
    {
      "deck_rank": 0,
      "is_patch_trend": false,
      "reason": "string"      // AI 생성 추천 이유
    }
  ]
}
</response-body>

</ai-to-spring>

<!-- ──────────────────────────────────────────────
     4. Spring 프록시 엔드포인트 스펙 (프론트 → Spring)
────────────────────────────────────────────── -->
<spring-proxy-endpoint>

<endpoint>GET /api/ai/recommend</endpoint>

<query-params>
- gameName: string (필수) — 소환사 게임 이름
- tagLine: string (필수) — 소환사 태그라인
</query-params>

<response>ApiResponse&lt;AiRecommendResponse&gt;</response>

<error-cases>
- 소환사 없음: 404
- 전적 없음: 200 (빈 데이터로 응답, AI 서버 호출 생략)
- AI 서버 타임아웃: 200 (fallback 응답, 에러 노출 안 함)
</error-cases>

<spring-files-to-create>
- AiRecommendController.java         — GET /api/ai/recommend
- AiRecommendService.java            — 전적 수집 + AI 서버 호출
- AiRecommendResponse.java           — 응답 DTO (AI 서버 응답과 동일 구조)
- AiServerClient.java                — AI 서버 HTTP 호출 (RestClient or WebClient)
</spring-files-to-create>

</spring-proxy-endpoint>

<!-- ──────────────────────────────────────────────
     5. AI 채팅 API 계약
────────────────────────────────────────────── -->
<chat-api>

<endpoint>POST http://ai-server:8000/api/chat</endpoint>

<request-body>
{
  "messages": [
    { "role": "user" | "assistant", "content": "string (max 2000)" }
  ],
  "context": {
    "summoner_name": "string | null",
    "tag_line": "string | null",
    "stats_summary": "string | null (max 512)",
    "good_traits": ["string"] | null,
    "bad_traits": ["string"] | null,
    "recent_matches": "string | null (max 5000)",
    "top_champions": ["string"] | null
  } | null
}
</request-body>

<response-body>
{ "reply": "string" }
</response-body>

<notes>
- messages는 최대 20개
- context는 프론트에서 초기 20게임 기준으로 구성하여 Spring이 그대로 전달
- context가 null이면 일반 TFT 지식 질문으로 처리
- AI 서버 오류 시 fallback 문자열 반환 (HTTP 200)
- OpenAI API 키 미설정 시에도 fallback 반환
</notes>

<spring-proxy>
- Spring 엔드포인트: POST /api/ai/chat
- Spring이 AiChatRequest를 그대로 AI 서버로 프록시
- AI 서버 응답을 ApiResponse&lt;AiChatResponse&gt;로 래핑하여 반환
- 타임아웃: 30초 (readTimeout)
</spring-proxy>

</chat-api>

<!-- ──────────────────────────────────────────────
     6. 환경변수
────────────────────────────────────────────── -->
<env>
Spring (application-docker.yml):
  ai.server.url: ${AI_SERVER_URL:http://ai-server:8000}
  ai.server.timeout-seconds: 10

AI 서버 (.env):
  OPENAI_API_KEY=sk-...
  DATABASE_URL=postgresql+asyncpg://...
</env>

</spec>
