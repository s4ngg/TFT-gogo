# Guide/PatchNotes 로컬 스모크 데이터

이 폴더는 Guide와 PatchNotes API를 로컬에서 확인하기 위한 수동 DB 기준 데이터를 정의합니다.

## 운영 원칙

- 현재 스키마 관리는 수동 SQL로 진행합니다.
- `spring.jpa.hibernate.ddl-auto`는 `none`입니다.
- `backend/build.gradle`에는 Flyway/Liquibase가 설정되어 있지 않습니다.
- DB 스키마 변경 PR은 `docs/qa/db-schema-change-rules.md`를 따릅니다.
- `01_schema.sql`은 QA 신규 DB 부트스트랩 기준 DDL로 관리하며, ERD Cloud export SQL은 #419 해결 전까지 실행 DDL의 정본으로 보지 않습니다.
- 공개 Guide/PatchNotes API는 `RIOT_API_KEY` 없이 확인할 수 있습니다.
- 이 스모크 경로에서는 `ai-server/.env`가 선택 사항이며, MySQL, Redis, backend만 필요합니다.

## 로컬 전용 기본값

`docker-compose.yml`의 DB 비밀번호, 관리자 토큰, JWT secret 기본값은 로컬 스모크 전용 placeholder입니다. 운영 또는 공유 환경에서는 사용하지 말고 `MYSQL_PASSWORD`, `ADMIN_SECRET_TOKEN`, `JWT_SECRET` 같은 환경변수로 덮어써야 합니다.

## 인프라 시작

Docker Compose는 로컬 MySQL/STS backend가 이미 `3306`/`8080`을 사용 중이어도 충돌하지 않도록 MySQL을 기본 host port `3307`, backend를 기본 host port `8081`에 매핑합니다.

```powershell
docker compose up -d mysql redis
```

## 스키마와 시드 적용

host MySQL client를 사용할 때:

```powershell
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u tftuser -ptftpass tftgogo < backend/src/main/resources/db/local-smoke/01_schema.sql
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u tftuser -ptftpass tftgogo < backend/src/main/resources/db/local-smoke/02_seed.sql
```

터미널 히스토리에 비밀번호를 남기고 싶지 않다면 `-ptftpass` 대신 `-p`만 입력하고 프롬프트에서 비밀번호를 입력합니다.

MySQL container를 통해 실행할 때:

```powershell
Get-Content -Encoding UTF8 backend/src/main/resources/db/local-smoke/01_schema.sql | docker exec -i tftgogo-mysql mysql --default-character-set=utf8mb4 -utftuser -ptftpass tftgogo
Get-Content -Encoding UTF8 backend/src/main/resources/db/local-smoke/02_seed.sql | docker exec -i tftgogo-mysql mysql --default-character-set=utf8mb4 -utftuser -ptftpass tftgogo
```

## 백엔드 실행

`application*.yml` 파일은 로컬 전용으로 git에서 제외되어 있으므로 Docker Compose 경로를 권장합니다. backend service는 datasource, Redis, admin token, JWT, Riot placeholder 값을 `docker-compose.yml`의 환경변수로 받습니다.

Docker image는 profile을 고정하지 않고, 실행 시점의 `SPRING_PROFILES_ACTIVE` 값으로 profile을 결정합니다. Compose 기본값은 `docker`이며 필요하면 환경변수로 덮어쓸 수 있습니다.

```powershell
docker compose up --build backend
```

선택 사항인 STS/Gradle 경로는 같은 MySQL, Redis, admin token, JWT, Riot placeholder 값에 맞춘 로컬 `backend/src/main/resources/application-local.yml`을 준비한 뒤 `local` profile로만 실행합니다.

```powershell
cd backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

## 스모크 체크

```powershell
curl.exe http://localhost:8081/api/guide
curl.exe "http://localhost:8081/api/guide/traits?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/items?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/augments?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/champions?page=1&pageSize=10&cost=4"

curl.exe http://localhost:8081/api/patch-notes
curl.exe "http://localhost:8081/api/patch-notes/17.3/changes?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/patch-notes/17.3/changes?category=CHAMPION&impact=HIGH"

curl.exe -H "X-Admin-Token: local-admin-token" http://localhost:8081/api/admin/guides
curl.exe -H "X-Admin-Token: local-admin-token" http://localhost:8081/api/admin/patch-notes
```

기대 결과: Guide와 PatchNotes 공개 API가 frontend fallback data 없이 시드된 `17.3` 데이터를 반환합니다.
