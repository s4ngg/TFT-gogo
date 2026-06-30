<spec domain="auth">

<purpose>
JWT 기반 일반 회원 인증 흐름.
일반 signup/login API는 짧은 수명의 access token과 HttpOnly refresh-token cookie를 발급하고, 인증 요청은 JwtAuthenticationFilter에서 처리한다.
</purpose>

<routes>
- POST /api/v1/auth/signup -> 회원가입
- POST /api/v1/auth/login  -> 로그인
- POST /api/v1/auth/refresh -> HttpOnly refreshToken cookie 기반 accessToken 재발급
- POST /api/v1/auth/logout -> accessToken 차단, refreshToken 세션 폐기, refresh cookie 삭제
- GET  /api/v1/auth/social/{provider} -> 소셜 로그인 시작 URL 조회
- GET  /oauth2/authorization/{provider} -> Spring Security OAuth2 브라우저 리다이렉트 시작
- GET  /login/oauth2/code/{provider} -> Spring Security OAuth2 콜백
</routes>

<providers>
- 지원 provider: google, naver
- 카카오는 이메일 권한 승인 전까지 프론트 버튼, Swagger 설명, provider enum에서 제외한다.
</providers>

<api>
<backend>
- POST /api/v1/auth/signup -> SignupRequest, AuthResponse
- POST /api/v1/auth/login  -> LoginRequest, AuthResponse
- POST /api/v1/auth/refresh -> AuthResponse
- POST /api/v1/auth/logout -> Void
- GET  /api/v1/auth/social/{provider} -> SocialLoginStartResponse
- GET  /api/v1/members/me  -> MemberResponse (authenticated)
</backend>
<frontend>
- frontend/src/api/memberApi.ts
- frontend/src/api/socialAuth.ts
- frontend/src/store/useAuthStore.ts
- frontend/src/pages/Auth/OAuthCallbackPage.tsx
</frontend>
</api>

<auth-flow>
- 회원가입 성공 시 accessToken을 응답 body로 반환하고 refreshToken은 HttpOnly cookie로 설정한다.
- 로그인 성공 시 accessToken을 응답 body로 반환하고 refreshToken은 HttpOnly cookie로 설정한다.
- 소셜 로그인은 `/api/v1/auth/social/{provider}`로 시작 URL을 받은 뒤 브라우저를 `/oauth2/authorization/{provider}`로 이동시킨다.
- 현재 QA/프론트 노출 provider는 Google, Naver이다. Kakao는 `account_email` 권한을 받을 때까지 지원 범위에서 제외한다.
- 운영 도메인은 `https://tftgogo.com`이며, 프론트 성공 콜백은 `https://tftgogo.com/oauth/callback`을 사용한다.
- 단일 ALB path 기반 배포에서는 `/oauth2/*`, `/login/oauth2/*`가 백엔드 target group으로 라우팅되어야 한다.
- 운영 소셜 로그인 시작 URL은 `APP_OAUTH2_AUTHORIZATION_BASE_URI=https://tftgogo.com` 기준으로 만든다.
- 소셜 로그인 시작 URL과 OAuth2 성공/실패 리다이렉트 URI는 http/https 절대 URL이어야 하며, 상대 경로, protocol-relative URL, userinfo가 포함된 URL은 거부한다.
- 실제 provider 인증 완료에는 각 provider client-id/client-secret, redirect-uri, 프론트 콜백 URI 설정이 필요하다.
- 소셜 로그인 시작 API는 provider enum이 지원되더라도 OAuth2 client registration 설정이 없으면 503 ApiResponse 실패를 반환하고, 프론트는 이메일 로그인을 안내한다.
- OAuth2 성공 시 백엔드는 `(socialProvider, socialId)` 기준으로 기존 회원을 찾거나 신규 소셜 회원을 생성하고 accessToken은 fragment로, refreshToken은 HttpOnly cookie로 전달한다.
- 프론트 `/oauth/callback`은 fragment에서 accessToken을 읽고 URL을 즉시 replace한 뒤 `/api/v1/members/me`로 현재 회원 정보를 복원한다.
- OAuth2 실패 시 백엔드는 프론트 로그인 URI에 whitelist `oauthError` code만 전달한다.
- accessToken은 Authorization: Bearer {token} 헤더로 전달한다.
- accessToken은 프론트 메모리 상태에만 저장한다. legacy localStorage `tftgogo-auth` 값은 제거한다.
- `/api/v1/auth/refresh`는 refreshToken cookie를 검증하고 refreshToken을 회전한 뒤 새 accessToken과 새 refreshToken cookie를 발급한다.
- axiosInstance는 인증이 필요한 API에서 401을 받으면 refresh를 1회 시도하고, 성공하면 원 요청을 1회 재시도한다.
- `/api/v1/auth/logout`은 accessToken jti를 만료 시각까지 blocklist에 저장하고 refreshToken 세션을 폐기한 뒤 refresh cookie를 삭제한다.
- JwtAuthenticationFilter는 토큰이 유효하면 SecurityContext에 userId 인증 정보를 저장한다.
- 토큰이 없거나 잘못된 경우 인증 정보를 만들지 않는다.
- 잘못된 토큰 때문에 500이 발생하면 안 된다.
</auth-flow>

<business-rules>
- 이메일은 중복될 수 없다.
- 닉네임은 회원가입과 소셜 회원 생성 시 서비스 중복 검증으로 중복 사용을 막는다.
- 비밀번호는 BCrypt로 암호화해서 저장한다.
- 로그인 실패 응답은 이메일 존재 여부를 노출하지 않도록 공통 인증 실패로 처리한다.
- 소셜 회원은 passwordHash가 null일 수 있으며 일반 이메일 로그인으로는 인증되지 않는다.
- 같은 이메일을 가진 일반 회원과 소셜 회원은 자동 연결하지 않는다.
- 소셜 계정은 `social_provider + social_id` 조합으로 중복 가입되지 않아야 한다.
- OAuth2 state 유지를 위한 세션은 OAuth2 브라우저 엔드포인트에서만 허용하고, `/api/**`는 JWT stateless 인증을 유지한다.
- OAuth2 성공/실패 핸들러는 리다이렉트 전 SecurityContext를 비우고 OAuth2 세션을 invalidate한다.
- 회원가입/로그인은 토큰 없이 접근 가능해야 한다.
- refresh/logout은 토큰 없이 접근 가능해야 한다. accessToken이 만료되어도 브라우저 refresh cookie를 정리할 수 있어야 한다.
- 소셜 로그인 시작 API와 Spring OAuth2 redirect endpoint는 토큰 없이 접근 가능해야 한다.
- 내 정보 조회는 인증된 사용자만 접근 가능해야 한다.
- JWT subject에는 numeric userId 문자열을 넣는다.
- JWT accessToken에는 jti, audience, tokenVersion claim이 있어야 한다.
- JWT subject가 숫자가 아니면 인증 실패로 처리한다.
- blocklist에 등록된 accessToken, 탈퇴/비활성 회원, tokenVersion이 맞지 않는 accessToken은 인증 실패로 처리한다.
- refreshToken 원문은 DB에 저장하지 않고 SHA-256 hash만 `refresh_token_sessions.token_hash`에 저장한다.
- 폐기된 refreshToken이 재사용되면 동일 회원의 활성 refreshToken 세션을 모두 폐기한다.
- JWT 설정값은 앱 기동 시 검증한다.
- 운영 CORS 허용 origin에는 `https://tftgogo.com`, `https://www.tftgogo.com`을 포함한다.
- Swagger annotation은 AuthController에 직접 작성하지 않고 AuthControllerDocs에 작성한다.
- Controller 응답 타입은 ResponseEntity&lt;ApiResponse&lt;T&gt;&gt;를 사용한다.
</business-rules>

<backend-structure>
- Auth controller: backend/src/main/java/com/tftgogo/domain/member/controller/AuthController.java
- Auth Swagger docs: backend/src/main/java/com/tftgogo/domain/member/controller/docs/AuthControllerDocs.java
- Member controller: backend/src/main/java/com/tftgogo/domain/member/controller/MemberController.java
- Member Swagger docs: backend/src/main/java/com/tftgogo/domain/member/controller/docs/MemberControllerDocs.java
- Request DTOs: backend/src/main/java/com/tftgogo/domain/member/dto/request/
- Command DTOs: backend/src/main/java/com/tftgogo/domain/member/dto/command/
- Response DTOs: backend/src/main/java/com/tftgogo/domain/member/dto/response/
- Services: backend/src/main/java/com/tftgogo/domain/member/service/ and service/impl/
- Repository: backend/src/main/java/com/tftgogo/domain/member/repository/MemberRepository.java
- Entity: backend/src/main/java/com/tftgogo/domain/member/entity/Member.java
- JWT: backend/src/main/java/com/tftgogo/global/security/
- OAuth2: backend/src/main/java/com/tftgogo/global/security/oauth/
- Filter: backend/src/main/java/com/tftgogo/global/filter/JwtAuthenticationFilter.java
</backend-structure>

<validation>
- Signup API success: valid SignupRequest -> AuthResponse with accessToken.
- Signup API failure: duplicate email -> EMAIL_ALREADY_EXISTS.
- Signup API failure: duplicate nickname -> NICKNAME_ALREADY_EXISTS.
- Login API success: valid LoginRequest -> AuthResponse with accessToken.
- Login API failure: wrong email or password -> common invalid credential error.
- Login API failure: social-only member with null passwordHash -> common invalid credential error.
- Social start API success: supported provider -> absolute authorizationUrl.
- Social start API supported providers: google, naver.
- Social start API failure: authorizationUrl baseUrl is not an http/https absolute URL -> 400 INVALID_INPUT.
- Social start frontend failure: non-http(s), relative, protocol-relative, credentialed, or whitespace-containing authorizationUrl -> redirect is blocked before window.location.assign.
- Social start API failure: unsupported provider -> 400 INVALID_INPUT.
- Social start API failure: supported provider without OAuth2 client registration -> 503 SOCIAL_PROVIDER_NOT_CONFIGURED.
- Auth refresh success: valid refresh cookie -> rotated refresh cookie + AuthResponse with new accessToken.
- Auth refresh failure: missing/revoked/expired refresh cookie -> 401 UNAUTHORIZED.
- Logout success: access token blocklisted when present, refresh session revoked when present, refresh cookie cleared.
- Social OAuth success: provider attributes -> existing or new social member -> frontend callback with accessToken fragment and refreshToken cookie.
- Social OAuth failure: provider error/email missing/email conflict -> frontend login redirect with whitelist oauthError code.
- Social OAuth redirect config failure: authorizedRedirectUri/loginFailureRedirectUri is not an http/https absolute URL -> app startup validation fails, and runtime redirect builder throws INVALID_INPUT if reached.
- Local social OAuth setup: copy the non-secret backend/src/main/resources/application-social-login.yml.example blocks into ignored application-local.yml, register Google/Naver provider console callbacks to backend /login/oauth2/code/{provider}, and never commit or share real client-secret values.
- Production social OAuth setup: register `https://tftgogo.com/login/oauth2/code/google`, `https://tftgogo.com/login/oauth2/code/naver`, and frontend callback `https://tftgogo.com/oauth/callback`.
- Social OAuth callback frontend success: accessToken fragment -> memory token set -> GET /api/v1/members/me user restore.
- Frontend auth restore success without memory token: refresh cookie -> /api/v1/auth/refresh -> GET /api/v1/members/me.
- Frontend auth restore failure: refresh or /api/v1/members/me 401/403 -> memory accessToken is cleared so logged-in UI is not shown with an invalid token.
- Auth request validation failure: invalid request body -> common validation error response.
- JWT success: valid Bearer token -> SecurityContext contains authenticated userId.
- JWT failure: missing token -> no authentication is created.
- JWT failure: invalid token -> no authentication is created and no 500 response occurs.
- JWT failure: non-numeric subject -> no authentication is created and no 500 response occurs.
- AuthController must not contain business logic; it delegates to MemberService.
- Backend verification uses compileJava or test.
</validation>

<out-of-scope>
- 일반 계정과 소셜 계정 자동 연결
</out-of-scope>

</spec>
