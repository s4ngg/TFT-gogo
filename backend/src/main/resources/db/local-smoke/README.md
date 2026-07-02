# v0.4 로컬 스모크 DB 기준

이 폴더는 QA 전에 새 MySQL DB에서 주요 백엔드 기능이 최소 동작하도록 Flyway callback seed 데이터를 정의합니다.

## 운영 원칙

- 스키마 관리는 Flyway 버전 마이그레이션(`db/migration/V*.sql`)으로 진행합니다.
- `spring.jpa.hibernate.ddl-auto`는 `validate`입니다 — 엔티티와 스키마가 불일치하면 앱 시작 시 실패합니다.
- `backend/build.gradle`에 `flyway-core`, `flyway-mysql` 의존성이 포함되어 있습니다.
- DB 스키마 변경 PR은 `docs/qa/db-schema-change-rules.md`를 따릅니다.
- `01_schema.sql`은 참조용 전체 DDL 스냅샷이며, 실제 스키마 생성은 Flyway `db/migration/V1__init_schema.sql`이 수행합니다.
- `afterMigrate__seed.sql`은 Flyway `afterMigrate` callback으로, 마이그레이션 완료 후 자동 실행되어 스모크 시드 데이터를 삽입합니다.
- ERD Cloud export SQL은 #419 해결 전까지 실행 DDL의 정본으로 보지 않습니다.
- 공개 Guide/PatchNotes API와 Auth/Party 기본 흐름은 `RIOT_API_KEY` 없이 확인할 수 있습니다.
- 소셜 OAuth E2E는 DB만으로 완료되지 않으며 provider `client-id`/`client-secret`과 callback URL 설정이 필요합니다.
- 이 스모크 경로에서는 `ai-server/.env`가 선택 사항이며, MySQL, backend만 필요합니다.
- 자세한 DDL/ERD 기준과 QA 범위는 `docs/qa/v0.4-db-ddl-eld.md`를 확인합니다.

## Flyway 마이그레이션 계약

### 신규 DB (fresh)

`V1__init_schema.sql`부터 순차 적용됩니다. `baseline-on-migrate=false`이므로 별도 baseline 없이 Flyway가 V1부터 실행합니다.

### 기존 DB (V16까지 수동 적용된 상태)

기존 DB에는 Flyway history가 없으므로 먼저 수동으로 baseline을 설정해야 합니다.

```bash
flyway baseline -baselineVersion=16
```

이후 앱을 시작하면 V17부터 자동 적용됩니다. `baseline-on-migrate=false`이므로 이 단계를 건너뛰면 앱 시작이 실패합니다.

## 로컬 전용 기본값

`docker-compose.yml`의 DB 비밀번호, JWT secret, AI internal secret 기본값은 로컬 개발 편의용 placeholder입니다. 운영 또는 공유 환경에서는 사용하지 말고 `MYSQL_PASSWORD`, `JWT_SECRET`, `AI_SERVER_INTERNAL_SECRET`, `ADMIN_BOOTSTRAP_PASSWORD` 같은 값을 외부 secret으로 주입해야 합니다.

`db/local-smoke` seed는 운영 기본 경로에 포함하지 않습니다. 로컬 스모크 데이터가 필요할 때만 `docker-compose.local-smoke.yml` override를 함께 지정합니다.

## 인프라 시작

Docker Compose는 로컬 MySQL/STS backend가 이미 `3306`/`8080`을 사용 중이어도 충돌하지 않도록 MySQL을 기본 host port `3307`, backend를 기본 host port `8081`에 매핑합니다.

```powershell
docker compose up -d mysql redis
```

## 스키마와 시드 적용

스키마는 backend 시작 시 Flyway가 자동으로 적용합니다. 시드는 로컬 스모크 override를 함께 지정했을 때만 적용합니다.

- **스키마**: `classpath:db/migration`의 `V*.sql` 파일을 순차 실행
- **시드**: `classpath:db/local-smoke`의 `afterMigrate__seed.sql` callback을 마이그레이션 완료 후 실행

기본 Docker Compose backend 서비스는 `SPRING_FLYWAY_LOCATIONS`를 `classpath:db/migration`으로 둡니다.
로컬 스모크 seed가 필요하면 아래처럼 override를 함께 사용합니다.

```powershell
docker compose -f docker-compose.yml -f docker-compose.local-smoke.yml up --build backend
```

`17.3 Local Patch`는 fallback smoke 데이터이며, Riot import 패치가 이미 있으면 current 패치를 덮어쓰지 않습니다.

기존 DB를 완전히 초기화하려면 먼저 아래 명령을 실행합니다.

주의: `docker compose down -v`는 Compose named volume을 삭제하므로 로컬 QA DB 데이터가 사라집니다.

```powershell
docker compose down -v
```

## 적용 검증

2026-06-16 기준 MySQL 8.0.44 임시 DB에서 Flyway 마이그레이션 적용을 확인했습니다.

- 생성 테이블: 22개
- `chat_rooms` seed: 4개
- split guide seed: 4개
- `patch_notes` seed: 1개
- `patch_note_changes` seed: 2개
- `backend` `compileJava`: Pass

## 백엔드 실행

`application*.yml` 파일은 로컬 전용으로 git에서 제외되어 있으므로 Docker Compose 경로를 권장합니다. backend service는 datasource, Redis, admin token, JWT, Riot placeholder 값을 `docker-compose.yml`의 환경변수로 받습니다.

Docker image는 profile을 고정하지 않고, 실행 시점의 `SPRING_PROFILES_ACTIVE` 값으로 profile을 결정합니다. Compose 기본값은 `docker`이며 필요하면 환경변수로 덮어쓸 수 있습니다.

```powershell
docker compose up --build backend
```

로컬 스모크 seed까지 넣어 확인하려면 override를 함께 지정합니다.

```powershell
docker compose -f docker-compose.yml -f docker-compose.local-smoke.yml up --build backend
```

선택 사항인 STS/Gradle 경로는 같은 MySQL, Redis, admin token, JWT, Riot placeholder 값에 맞춘 로컬 `backend/src/main/resources/application-local.yml`을 준비한 뒤 `local` profile로만 실행합니다.

```powershell
cd backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

## 최신 Guide/PatchNotes import

최신 게임가이드와 패치노트를 로컬 DB에 넣어야 할 때는 backend 시작 전에 import 스위치를 켭니다.

- PatchNotes import는 Riot 공식 패치노트 목록에서 최신 항목을 가져와 current 패치로 저장합니다.
- Guide import는 `APP_GUIDE_CDRAGON_PATCH_VERSION=latest` 기준으로 current 패치노트 버전을 사용합니다.
- CDragon 세트 번호와 mutator를 지정하지 않으면 CDragon 응답에서 가장 최신 TFT 세트를 자동 선택합니다.

```powershell
$env:APP_PATCH_NOTE_SCHEDULER_ENABLED="true"
$env:APP_PATCH_NOTE_SCHEDULER_STARTUP_IMPORT="true"
$env:APP_PATCH_NOTE_SCHEDULER_LOCALE="ko-kr"
$env:APP_PATCH_NOTE_SCHEDULER_CURRENT="true"

$env:APP_GUIDE_CDRAGON_ENABLED="true"
$env:APP_GUIDE_CDRAGON_STARTUP_IMPORT="true"
$env:APP_GUIDE_CDRAGON_PATCH_VERSION="latest"
$env:APP_GUIDE_CDRAGON_INCLUDE_CHAMPIONS="true"
$env:APP_GUIDE_CDRAGON_INCLUDE_TRAITS="true"
$env:APP_GUIDE_CDRAGON_INCLUDE_ITEMS="true"
$env:APP_GUIDE_CDRAGON_INCLUDE_AUGMENTS="true"

docker compose up --build backend
```

## 스모크 체크

```powershell
curl.exe http://localhost:8081/api/guide
curl.exe "http://localhost:8081/api/guide/traits?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/items?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/augments?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/champions?page=1&pageSize=10&cost=4"

curl.exe http://localhost:8081/api/patch-notes
$patchVersion = (Invoke-RestMethod http://localhost:8081/api/patch-notes).data[0].version
curl.exe "http://localhost:8081/api/patch-notes/$patchVersion/changes?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/patch-notes/$patchVersion/changes?category=CHAMPION&impact=HIGH"
```

기대 결과:

- Guide와 PatchNotes 공개 API가 frontend fallback data 없이 최신 import 데이터를 반환합니다.
- Auth signup/login/me, Party create/join/cancel이 임시 테이블 생성 없이 동작합니다.
- Chat GET messages/SSE는 공개 접근, POST message는 로그인 토큰으로 동작합니다.
- Chat DB 테이블은 ELD 예약 테이블이며 현재 MVP 런타임은 in-memory chat을 사용합니다.
