<spec domain="deploy-routing">

<purpose>
AWS ALB + Route53 배포 도메인 기준 라우팅 계약.
</purpose>

<domain>
- Primary: https://tftgogo.com
- Alias: https://www.tftgogo.com
- OAuth는 `APP_OAUTH2_AUTHORIZATION_BASE_URI=https://tftgogo.com` 기준으로 시작해 provider callback mismatch를 피한다.
</domain>

<alb>
- 단일 ALB에서 host 기반이 아니라 path 기반으로 라우팅한다.
- HTTP:80은 HTTPS:443으로 301 리다이렉트한다.
- HTTPS:443 기본 대상 그룹은 `tftgogo-frontend-tg`다.
</alb>

<routes>
- `/api`, `/api/*` -> `tftgogo-backend-tg`
- `/actuator/health`, `/actuator/health/*` -> `tftgogo-backend-tg`
- `/oauth2/*` -> `tftgogo-backend-tg`
- `/login/oauth2/*` -> `tftgogo-backend-tg`
- 그 외 모든 경로 -> `tftgogo-frontend-tg`
</routes>

<backend>
- ALB 뒤의 Spring Boot는 `server.forward-headers-strategy=framework`를 사용한다.
- 이 설정이 없으면 OAuth2 시작 URL이나 콜백 URL이 내부 HTTP 주소로 만들어질 수 있다.
- 운영 환경변수:
  - `APP_CORS_ALLOWED_ORIGINS=https://tftgogo.com,https://www.tftgogo.com`
  - `APP_OAUTH2_AUTHORIZATION_BASE_URI=https://tftgogo.com`
  - `APP_OAUTH2_AUTHORIZED_REDIRECT_URI=https://tftgogo.com/oauth/callback`
  - `APP_OAUTH2_LOGIN_FAILURE_REDIRECT_URI=https://tftgogo.com/login`
  - `AI_SERVER_URL=http://<internal-ai-service>:8000`
  - `SERVER_FORWARD_HEADERS_STRATEGY=framework`
  - `SPRING_FLYWAY_LOCATIONS=classpath:db/migration`
- 백엔드 ALB target group health check path는 `/actuator/health`를 사용한다.
- 운영에서는 `db/local-smoke` Flyway callback을 포함하지 않는다. 로컬 스모크 seed는 `docker-compose.local-smoke.yml`로만 명시적으로 켠다.
- 운영 secret은 compose 기본값을 사용하지 않고 ECS task definition의 secrets, SSM Parameter Store, 또는 Secrets Manager로 주입한다.
</backend>

<frontend>
- Vite API base URL은 기본값 `/api`를 사용한다.
- 운영에서 `VITE_API_URL` 또는 `VITE_API_BASE_URL`을 별도 API 서브도메인으로 지정하지 않는다.
- 브라우저는 AI 서버를 직접 호출하지 않고 Spring의 `/api/ai/recommend` 프록시를 호출한다.
</frontend>

<ai-server>
- AI 서버는 공개 ALB path로 노출하지 않는다.
- Spring 백엔드가 내부 `AI_SERVER_URL`로 AI 서버의 `/api/analyze/with-meta`를 호출한다.
- CORS는 브라우저 정책일 뿐 공개 엔드포인트 보호 수단이 아니다.
</ai-server>

<swagger>
- Swagger UI와 OpenAPI 문서는 백엔드 경로다.
- ALB에서 Swagger를 공개할 경우 `/swagger-ui.html`, `/swagger-ui/*`, `/v3/api-docs`, `/v3/api-docs/*`를 `tftgogo-backend-tg`로 라우팅해야 한다.
</swagger>

<oauth>
- Provider callback:
  - `https://tftgogo.com/login/oauth2/code/google`
  - `https://tftgogo.com/login/oauth2/code/naver`
- Frontend callback:
  - `https://tftgogo.com/oauth/callback`
</oauth>

</spec>
