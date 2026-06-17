# DB Schema Change Rules

이 문서는 DB 테이블/컬럼/제약조건 변경이 있는 PR에서 DDL, ERD Cloud, QA 확인 범위를 숨기지 않기 위한 기준이다.

## 기준

- 실행 가능한 QA 신규 DB 부트스트랩 기준 DDL은 `backend/src/main/resources/db/local-smoke/01_schema.sql`이다.
- seed/init 기준은 `backend/src/main/resources/db/local-smoke/02_seed.sql`이다.
- 현재 프로젝트는 Flyway/Liquibase 자동 migration을 사용하지 않는다. `db/migration/*.sql` 파일이 생기더라도 migration 도구가 도입되기 전까지는 수동 적용 SQL로 취급한다.
- `spring.jpa.hibernate.ddl-auto=none` 기준에서 서버가 떠야 한다. JPA Entity만 고치고 DDL 반영을 생략하면 QA 기준을 만족하지 못한다.
- ERD Cloud는 다이어그램 동기화와 팀 협의 추적용이다. #419의 ERD Cloud export SQL 실행 문제가 해결되기 전까지 ERD export SQL을 실행 DDL의 source of truth로 보지 않는다.

## PR 작성 규칙

DB 스키마 변경이 없는 PR은 PR 템플릿의 `DB 스키마 변경 없음`에 체크하고 N/A 사유를 적는다.

DB 스키마 변경이 있는 PR은 아래 중 하나를 PR 본문에 남긴다.

- Entity와 DDL을 같은 PR에서 함께 반영했다.
- Entity는 바뀌었지만 DDL 반영은 후속 이슈 `#___`로 분리했다.
- DDL만 바뀌었고 Entity/API 영향은 없다.
- seed/init SQL 변경이 필요 없으며 그 이유를 적었다.
- `ddl-auto=none` 기준 새 DB 또는 새 Docker volume에서 서버 기동을 확인했다.
- 확인하지 못했다면 blocker 또는 후속 이슈 `#___`를 남겼다.

## DDL과 ERD 담당 흐름

- DB 변경 PR 작성자가 DDL 반영 여부를 1차로 책임진다.
- 기준 DDL은 `local-smoke/01_schema.sql`에 반영하거나, 같은 PR에서 반영하지 못하면 후속 이슈를 만든다.
- seed 데이터가 필요한 변경은 `local-smoke/02_seed.sql` 반영 여부를 PR 본문에 적는다.
- ERD Cloud는 PR 작성자가 다음 중 하나를 남긴다.
  - ERD Cloud 반영 완료
  - ERD Cloud 반영 불필요와 사유
  - ERD Cloud 후속 이슈 링크
  - ERD export SQL 재생성 여부 또는 미반영 사유
- ERD Cloud 반영 여부만 체크하지 말고, 리뷰어가 추적할 수 있는 이슈 번호나 사유를 함께 남긴다.

## QA mismatch 이슈 분리 기준

아래 항목은 같은 PR에서 고치거나 blocker 이슈로 분리한다.

- `ddl-auto=none` 기준 서버 기동 실패
- 빈 QA DB 또는 새 Docker volume에서 schema/seed init 실패
- API QA가 막히는 테이블 또는 컬럼 누락
- JPA Entity와 DDL의 타입, nullable, default, PK, FK, unique, index, CHECK constraint 불일치
- `ON DELETE` 또는 `ON UPDATE` 동작 불일치
- charset/collation, `DATETIME(6)` 정밀도 차이처럼 데이터 저장 결과가 달라질 수 있는 차이
- 데이터 무결성 위험이 있는 제약조건 누락

아래 항목은 후속 이슈로 분리할 수 있다.

- ERD Cloud 화면 배치나 표시 순서 정리
- #419처럼 ERD export SQL 자체의 포맷/실행 오류
- 운영 영향 없는 문서 표기 차이
- migration 도구 도입처럼 별도 설계가 필요한 구조 변경
- 현재 QA 범위를 막지 않는 legacy snapshot-only 차이

후속 이슈에는 최소한 테이블명, 컬럼명, 기대 DDL, 실제 DDL 또는 ERD 상태, QA 영향, 마일스톤을 남긴다.

## 비밀값 기록 금지

PR 본문, 첨부 이미지, Discord, 문서에는 실제 비밀값을 남기지 않는다.

금지 예시는 다음과 같다.

- OAuth client secret
- DB password
- JWT secret
- GitHub token
- SSH private key
- Discord webhook
- Riot API key
- AWS access key 또는 secret key
- OpenAI/Anthropic API key
- admin token

필요하면 환경변수명, placeholder, `<redacted>`만 사용한다. `local-smoke` README의 로컬 전용 placeholder 값은 실제 secret이 아니지만, 운영 또는 공유 환경의 실제 값으로 재사용하지 않는다.

## 관련 이슈와 PR

- #419: ERD Cloud export SQL 실행 오류 추적
- #427: QA DB DDL/ELD 기준 정리
- #428: `local-smoke` DDL/ERD 체크리스트 반영 PR
- #441: 이 문서와 PR 템플릿으로 DB 변경 PR 체크 규칙을 확정
