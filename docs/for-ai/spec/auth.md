<spec domain="auth">

<purpose>
JWT 기반 일반 회원 인증 흐름.
General signup/login APIs issue access tokens, and authenticated requests are resolved through JwtAuthenticationFilter.
</purpose>

<routes>
- POST /api/v1/auth/signup -> 회원가입
- POST /api/v1/auth/login  -> 로그인
</routes>

<api>
<backend>
- POST /api/v1/auth/signup -> SignupRequest, AuthResponse
- POST /api/v1/auth/login  -> LoginRequest, AuthResponse
- GET  /api/v1/members/me  -> MemberResponse (authenticated)
</backend>
<frontend>
- frontend/src/api/memberApi.ts
- frontend/src/store/useAuthStore.ts
</frontend>
</api>

<auth-flow>
- 회원가입 성공 시 accessToken을 발급한다.
- 로그인 성공 시 accessToken을 발급한다.
- accessToken은 Authorization: Bearer {token} 헤더로 전달한다.
- JwtAuthenticationFilter는 토큰이 유효하면 SecurityContext에 userId 인증 정보를 저장한다.
- 토큰이 없거나 잘못된 경우 인증 정보를 만들지 않는다.
- 잘못된 토큰 때문에 500이 발생하면 안 된다.
</auth-flow>

<business-rules>
- 이메일은 중복될 수 없다.
- 비밀번호는 BCrypt로 암호화해서 저장한다.
- 로그인 실패 응답은 이메일 존재 여부를 노출하지 않도록 공통 인증 실패로 처리한다.
- 회원가입/로그인은 토큰 없이 접근 가능해야 한다.
- 내 정보 조회는 인증된 사용자만 접근 가능해야 한다.
- JWT subject에는 numeric userId 문자열을 넣는다.
- JWT subject가 숫자가 아니면 인증 실패로 처리한다.
- JWT 설정값은 앱 기동 시 검증한다.
- Swagger annotations belong in AuthControllerDocs, not directly in AuthController.
- Controller response type must be ResponseEntity&lt;ApiResponse&lt;T&gt;&gt;.
</business-rules>

<backend-structure>
- Auth controller: backend/src/main/java/com/tftgogo/domain/member/controller/AuthController.java
- Auth Swagger docs: backend/src/main/java/com/tftgogo/domain/member/controller/docs/AuthControllerDocs.java
- Member controller: backend/src/main/java/com/tftgogo/domain/member/controller/MemberController.java
- Member Swagger docs: backend/src/main/java/com/tftgogo/domain/member/controller/docs/MemberControllerDocs.java
- Request DTOs: backend/src/main/java/com/tftgogo/domain/member/dto/request/
- Response DTOs: backend/src/main/java/com/tftgogo/domain/member/dto/response/
- Services: backend/src/main/java/com/tftgogo/domain/member/service/ and service/impl/
- Repository: backend/src/main/java/com/tftgogo/domain/member/repository/MemberRepository.java
- Entity: backend/src/main/java/com/tftgogo/domain/member/entity/Member.java
- JWT: backend/src/main/java/com/tftgogo/global/security/
- Filter: backend/src/main/java/com/tftgogo/global/filter/JwtAuthenticationFilter.java
</backend-structure>

<validation>
- Signup API success: valid SignupRequest -> AuthResponse with accessToken.
- Signup API failure: duplicate email -> EMAIL_ALREADY_EXISTS.
- Login API success: valid LoginRequest -> AuthResponse with accessToken.
- Login API failure: wrong email or password -> common invalid credential error.
- Auth request validation failure: invalid request body -> common validation error response.
- JWT success: valid Bearer token -> SecurityContext contains authenticated userId.
- JWT failure: missing token -> no authentication is created.
- JWT failure: invalid token -> no authentication is created and no 500 response occurs.
- JWT failure: non-numeric subject -> no authentication is created and no 500 response occurs.
- AuthController must not contain business logic; it delegates to MemberService.
- Backend verification uses compileJava or test.
</validation>

<out-of-scope>
- Refresh Token
- 로그아웃
- 소셜 로그인
</out-of-scope>

</spec>
