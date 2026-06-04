<spec domain="ai-recommend">

<purpose>
AI-powered deck recommendation based on current units and items.
Page: AiRecommend (/ai-recommend).
Backend: ai-server (FastAPI) — see spec-ai.md for ai-server conventions.
</purpose>

<routes>
- /ai-recommend → AI recommendation input + result page
</routes>

<api>
<frontend>
- frontend/src/api/aiRecommendApi.ts — calls ai-server through backend proxy
</frontend>
<flow>
1. User inputs current units / items.
2. Frontend calls backend proxy endpoint.
3. Backend forwards to ai-server.
4. ai-server runs RAG pipeline (pgvector + OpenAI Embeddings + LangChain) and returns recommendation.
5. Backend normalizes and returns result to frontend.
</flow>
</api>

<business-rules>
- Recommendation output includes: unit composition, item placement, positioning.
- Frontend never calls ai-server directly — always goes through backend proxy.
- ai-server errors (timeout, invalid input) must surface as clear error states in the UI.
- Augment recommendations are NOT included (Riot API does not provide augment data).
</business-rules>

<frontend-structure>
- frontend/src/pages/AiRecommend/
</frontend-structure>

</spec>
