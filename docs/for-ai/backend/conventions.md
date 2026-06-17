<spec domain="backend">

<stack>Java · Spring Boot · JPA · Log4j2</stack>

<conventions>

<exception>
- Do not create domain-specific Exception classes.
- Use BusinessException(ErrorCode.XXX) pattern.
- All exceptions must be handled in GlobalExceptionHandler.
- Riot API 429 레이트 리밋 발생 시 BusinessException(ErrorCode.RIOT_API_RATE_LIMIT, retryAfterSeconds)을 사용한다.
  retryAfterSeconds > 0이면 GlobalExceptionHandler가 Retry-After 응답 헤더를 자동으로 설정한다.
</exception>

<response>
- All API responses must use ApiResponse&lt;T&gt;.
- Return type: ResponseEntity&lt;ApiResponse&lt;T&gt;&gt;
- Success: ApiResponse.success("message", data)
- Failure: ApiResponse.fail("message", HttpStatus)
</response>

<dto>
- RequestDTO may include a toEntity() method.
- ResponseDTO must use from() or of() static factory methods.
- @NotNull / @NotBlank annotations are allowed only on RequestDTO, not ResponseDTO.
</dto>

<async>
- @Async void is forbidden. Return CompletableFuture&lt;Void&gt; instead (void cannot propagate exceptions).
- SimpleAsyncTaskExecutor is forbidden. Configure an explicit ThreadPoolTaskExecutor.
</async>

<code-quality>
- Do not return null. Use Optional + orElseThrow().
- System.out.println is forbidden. Use Log4j2 Logger.
- java.util.Random is forbidden. Use SecureRandom.
- Avoid @Value injection. Prefer @ConfigurationProperties.
- Do not filter large datasets with Java streams. Apply conditions at the DB query level.
</code-quality>

<database-change>
- When changing tables, columns, indexes, constraints, or seed/init data, follow docs/qa/db-schema-change-rules.md.
- Do not rely on JPA ddl-auto. The project runs with ddl-auto=none, and the executable QA bootstrap DDL is backend/src/main/resources/db/local-smoke/01_schema.sql.
- ERD Cloud is a sync/tracking artifact. Until #419 is resolved, do not treat exported ERD SQL as the executable DDL source of truth.
- Contract change rule: If an Entity change alters SQL schema, update 01_schema.sql in the same PR or create a blocking follow-up issue referenced in the PR.
</database-change>

<security>
- Never store passwords or auth codes in plain text. Use BCrypt.
- Never log JWT tokens.
- Bind JWT settings with `@ConfigurationProperties` using `jwt.secret` and `jwt.access-token-expiration-millis`.
- Validate JWT secret at startup as nonblank and at least 32 characters, and validate token expiration as positive.
</security>

<structure>
- Service implementations go in service/impl/.
- Swagger annotations belong only in XxxControllerDocs interface; Controller implements it.
</structure>

</conventions>

<logging>
<declaration>private static final Logger logger = LogManager.getLogger(Xxx.class);</declaration>
<levels>
- logger.info  — normal flow (success cases)
- logger.warn  — abnormal but system continues (failures, bad requests)
- logger.error — exception occurred (include exception object)
</levels>
<forbidden>passwords, JWT tokens, auth codes</forbidden>
</logging>

<testing>
- Unit-test the Service layer only. Controller, Entity, DTO, Repository tests are not required.
- Use given/when/then pattern.
- Annotations: @ExtendWith(MockitoExtension.class), @Mock, @InjectMocks
- No real DB or external service connections. Use Mocks.
- Test method names should be written in Korean per team convention.
  Example: 회원이_없으면_예외를_던진다()
</testing>

</spec>
