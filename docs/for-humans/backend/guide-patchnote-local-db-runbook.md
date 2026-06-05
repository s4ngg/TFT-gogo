# Guide/PatchNotes 로컬 DB 기동 runbook

## 목적

이 문서는 `게임가이드`와 `패치노트` 공개 API를 프론트 fallback 데이터가 아니라 로컬 MySQL 데이터로 확인하기 위한 절차다.

현재 팀 저장소에는 Flyway/Liquibase 같은 DB migration 도구가 없다. 따라서 #151 범위에서는 새 도구를 도입하지 않고 `수동 SQL + 적용 순서 문서화` 방식으로 진행한다.

## 이번 단계에서 하는 일

- `guides` 테이블을 만든다.
- `patch_notes`, `patch_changes` 테이블을 만든다.
- Guide 탭별 smoke 데이터 1개 이상을 넣는다.
  - traits
  - items
  - augments
  - champions
- PatchNotes 현재 패치 1개와 대표 변경사항을 넣는다.
  - category: `CHAMPION`, `TRAIT`, `ITEM`, `AUGMENT`, `SYSTEM`
  - type: `BUFF`, `NERF`, `ADJUST`, `NEW`
  - impact: `HIGH`, `MEDIUM`, `LOW`
- `RIOT_API_KEY` 없이도 `/api/guide`, `/api/patch-notes`를 확인한다.

## 이번 단계에서 하지 않는 일

- Riot API 전적 검색 실데이터 연동
- 메타 덱 대량 집계 실행
- ai-server FastAPI 기동
- 운영 배포용 migration 자동화

## 왜 수동 SQL인가

현재 `backend/build.gradle`에는 Flyway/Liquibase 의존성이 없다. `docs/for-humans/backend/constitution.md`도 migration 도구가 확정되기 전에는 테이블 변경 SQL, 적용 순서, 롤백 방법을 PR 또는 이슈에 명시하라고 정해두었다.

그래서 지금은 아래 순서가 가장 안전하다.

1. 로컬 MySQL에 SQL을 직접 적용한다.
2. STS에서 Spring Boot 서버만 켠다.
3. curl 또는 Swagger로 Guide/PatchNotes 공개 API를 확인한다.
4. 이후 관리자 API가 준비되면 SQL 직접 입력 대신 관리자 API로 데이터를 넣는다.

## SQL 파일

```text
docs/for-humans/backend/sql/guide-patchnote-local-schema.sql
docs/for-humans/backend/sql/guide-patchnote-local-seed.sql
docs/for-humans/backend/sql/guide-patchnote-local-reset.sql
```

각 파일의 역할은 다음과 같다.

| 파일 | 역할 |
| --- | --- |
| `guide-patchnote-local-schema.sql` | `guides`, `patch_notes`, `patch_changes` 테이블 생성 |
| `guide-patchnote-local-seed.sql` | 로컬 smoke test용 최소 데이터 입력 |
| `guide-patchnote-local-reset.sql` | 위 seed 데이터만 제거하는 선택용 reset |

## 로컬 MySQL 준비

이미 로컬 MySQL이 있다면 `tftgogo` 데이터베이스와 로컬 검증용 계정만 준비하면 된다.

| 항목 | 기본값 |
| --- | --- |
| 데이터베이스 | `tftgogo` |
| 사용자 | `tftuser` |
| 비밀번호 | `tftpass` |

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "CREATE DATABASE IF NOT EXISTS tftgogo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'tftuser'@'localhost' IDENTIFIED BY 'tftpass'; GRANT ALL PRIVILEGES ON tftgogo.* TO 'tftuser'@'localhost'; FLUSH PRIVILEGES;"
```

`CREATE USER IF NOT EXISTS`는 이미 존재하는 계정의 비밀번호를 바꾸지 않는다. 기존 `tftuser` 비밀번호를 초기화해야 할 때만 별도로 `ALTER USER`를 실행한다.

`docker-compose.yml`의 MySQL만 쓰고 싶다면 전체 compose를 올리지 말고 MySQL과 Redis만 올린다. 현재 `backend/Dockerfile`, `ai-server/Dockerfile`은 없으므로 전체 compose 기동은 이 단계의 확인 방법이 아니다.

```powershell
docker compose up -d mysql redis
```

## SQL 적용 순서

아래 명령은 리포지토리 루트에서 실행하는 PowerShell 기준이다. `mysql < file.sql` 형태는 PowerShell에서 헷갈릴 수 있으므로 `source`를 사용한다.

```powershell
mysql --default-character-set=utf8mb4 -u tftuser -p tftgogo -e "source ./docs/for-humans/backend/sql/guide-patchnote-local-schema.sql"
mysql --default-character-set=utf8mb4 -u tftuser -p tftgogo -e "source ./docs/for-humans/backend/sql/guide-patchnote-local-seed.sql"
```

로컬 MySQL 계정이 다르면 `-u tftuser` 부분과 `application-local.yml`의 username/password를 본인 환경에 맞춘다.

적용 후 최소 확인 쿼리는 다음과 같다.

```powershell
mysql --default-character-set=utf8mb4 -u tftuser -p tftgogo -e "SELECT guide_type, target_key, patch_version FROM guides ORDER BY sort_order;"
mysql --default-character-set=utf8mb4 -u tftuser -p tftgogo -e "SELECT version, is_current, is_active FROM patch_notes;"
mysql --default-character-set=utf8mb4 -u tftuser -p tftgogo -e "SELECT category, change_type, impact, target_key FROM patch_changes ORDER BY sort_order;"
```

## STS 로컬 설정

`backend/src/main/resources/application-local.yml`은 `.gitignore` 대상이라 커밋하지 않는다. 로컬에서만 아래처럼 만든다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tftgogo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: tftuser
    password: tftpass
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8080

riot:
  api-key: local-smoke-disabled
  kr-base-url: https://kr.api.riotgames.com
  asia-base-url: https://asia.api.riotgames.com

app:
  meta-deck:
    startup-aggregate: false
```

중요한 설정은 `app.meta-deck.startup-aggregate=false`다. 이 값을 넣지 않으면 서버 시작 시 메타 덱 스케줄러가 Riot API 대량 집계를 시도할 수 있다.

STS 실행 설정은 다음처럼 둔다.

| 항목 | 값 |
| --- | --- |
| Main class | `com.tftgogo.TftgogoApplication` |
| Active profile | `local` |
| Server port | `8080` |

`application.yml`을 복사해서 쓰는 경우에도 `RIOT_API_KEY`는 실제 키가 아니라 `local-smoke-disabled` 같은 더미 값으로 충분하다. Guide/PatchNotes 공개 API smoke test는 Riot API를 호출하지 않는다.

## API smoke test

서버가 켜진 뒤 PowerShell에서 아래를 확인한다.

```powershell
curl.exe http://localhost:8080/api/guide
curl.exe "http://localhost:8080/api/guide/traits?page=1&pageSize=5"
curl.exe "http://localhost:8080/api/guide/items?sortKey=pickRate&sortDir=desc&page=1&pageSize=5"
curl.exe "http://localhost:8080/api/guide/champions?cost=4&page=1&pageSize=5"
curl.exe http://localhost:8080/api/patch-notes
curl.exe "http://localhost:8080/api/patch-notes/17.3/changes?page=1&pageSize=10"
curl.exe "http://localhost:8080/api/patch-notes/17.3/changes?category=CHAMPION&type=BUFF&impact=HIGH&page=1&pageSize=5"
```

기대 결과는 다음과 같다.

| API | 기대 결과 |
| --- | --- |
| `/api/guide` | Guide 엔트리 4개 이상 반환 |
| `/api/guide/traits` | `다크 스타` 반환 |
| `/api/guide/items` | `쇼진의 창` 반환 |
| `/api/guide/champions?cost=4` | `징크스` 반환 |
| `/api/patch-notes` | `17.3 패치노트`와 `changeCount` 반환 |
| `/api/patch-notes/17.3/changes` | 변경사항 5개 반환 |
| `/api/patch-notes/17.3/changes?category=CHAMPION&type=BUFF&impact=HIGH` | `징크스` 변경사항 1개 반환 |

Swagger로 확인하려면 다음 주소를 사용한다.

```text
http://localhost:8080/swagger-ui/index.html
```

## 프론트에서 fallback 없이 확인하기

프론트 Vite 개발 서버는 기본 proxy가 `/api`를 `http://localhost:8080`으로 넘긴다. 따라서 `frontend/.env`의 `VITE_API_URL`, `VITE_API_BASE_URL`을 비워두면 된다.

```powershell
cd frontend
npm run dev
```

Guide 또는 PatchNotes 화면에서 fallback 배지가 사라지고 API 데이터가 보이면 성공이다.

## reset 방법

seed 데이터만 제거하려면 다음을 실행한다.

```powershell
mysql --default-character-set=utf8mb4 -u tftuser -p tftgogo -e "source ./docs/for-humans/backend/sql/guide-patchnote-local-reset.sql"
```

테이블까지 제거하는 SQL은 reset 파일에 주석으로만 남겨두었다. 로컬 smoke 데이터가 아닌 팀원 데이터가 들어간 뒤에는 테이블 drop을 실행하지 않는다.

## 다음 단계

이후(`#151` 참조)에는 SQL을 직접 넣는 방식에서 관리자 API 방식으로 넘어가는 것이 좋다.

- #161 게임가이드 관리자 API 추가
- #150 패치노트 관리자 API 추가
- #162도 같은 주제의 패치노트 관리자 API 이슈이므로 착수 전 하나로 정리한다.

관리자 API가 생기면 로컬 smoke seed는 초기 확인용으로만 쓰고, 실제 콘텐츠 추가/수정은 관리자 API로 처리한다.
