<spec domain="ai-server">

<stack>Python · FastAPI · LangChain · pgvector · OpenAI Embeddings API</stack>

<conventions>

<structure>
- api/      — router and endpoint definitions only
- services/ — business logic, RAG/recommendation logic
- models/   — Pydantic schemas
- core/     — configuration, DB connection, dependency injection
</structure>

<response>
- All responses must use a common BaseResponse-based schema.
</response>

<logging>
- print() is forbidden. Use the logging module.
</logging>

<rag>
- RAG pipeline follows: pgvector + OpenAI Embeddings API + LangChain.
</rag>

<external-api>
- Errors from AI server (failures, timeouts, invalid input) must be returned as clear, distinguishable error responses.
- Backend validates and proxies frontend requests to ai-server; ai-server responses are normalized before returning to frontend.
</external-api>

</conventions>

</spec>
