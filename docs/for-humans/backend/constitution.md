# constitution.md - 백엔드 프로젝트 규칙 정의

> 백엔드에서 어떻게 만들 것인가를 정의한다.
> 팀 전체가 같은 기준으로 API, 도메인, 예외, 테스트를 작성하기 위한 규칙이다.

---

## 문서 기준

- 기준 브랜치: `develop`
- 기준 시점: 2026-06-01
- 대상 모듈: `backend/`
- 백엔드 역할: Spring Boot 기반 API 서버, Riot API 연동, 내부 집계 데이터 관리, 관리자 큐레이션 API 제공

---

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.3.0 |
| 빌드 | Gradle |
| Web | Spring Web |
| Persistence | Spring Data JPA |
| Security | Spring Security |
| Validation | Spring Validation |
| DB | MySQL |
| Cache/Infra | Redis |
| 문서화 | SpringDoc OpenAPI |
| Logging | Log4j2 |
| 테스트 | JUnit 5, Mockito, Spring Security Test |

---

## 패키지 구조 규칙

도메인별 코드는 `com.tftgogo.domain.<domain>` 아래에 둔다.

```text
domain/<domain>/
  controller/
    docs/
  dto/
    request/
    response/
  entity/
  repository/
  service/
    impl/
```

- Controller는 HTTP 입출력만 담당한다.
- Swagger 어노테이션은 `controller/docs/XxxControllerDocs` 인터페이스에 작성하고, Controller는 이를 구현한다.
- Service 인터페이스는 `service/`, 구현체는 `service/impl/`에 둔다.
- Request DTO는 `dto/request/`, Response DTO는 `dto/response/`에 둔다.
- Entity는 DB 테이블 매핑만 담당하고 화면 응답 포맷을 직접 알지 않는다.
- Repository는 DB 조회 조건을 명시한다. 대량 데이터를 가져온 뒤 Java Stream으로 필터링하는 방식은 지양한다.

---

## 공통 응답 규칙

- 모든 API 응답은 `ResponseEntity<ApiResponse<T>>`로 반환한다.
- 성공 응답은 `ApiResponse.success(message, data)` 또는 `ApiResponse.success(message)`를 사용한다.
- 실패 응답은 예외를 던지고 `GlobalExceptionHandler`에서 공통 처리한다.
- Controller에서 임의의 Map 응답이나 raw DTO를 직접 반환하지 않는다.
- 목록 API는 프론트 계약에 맞춰 명확한 응답 DTO를 둔다.
- 페이지 응답은 `items`, `page`, `pageSize`, `totalItems`, `totalPages`를 기본 필드로 사용한다.
- 프론트에 노출하는 페이지 번호는 1부터 시작한다. 내부 `PageRequest`를 사용할 때만 0부터 시작하는 인덱스로 변환한다.

---

## 예외 처리 규칙

- 도메인별 Exception 클래스를 새로 만들지 않는다.
- 비즈니스 오류는 `BusinessException(ErrorCode.XXX)` 패턴을 사용한다.
- 리소스 없음은 `Optional.orElseThrow()`로 처리한다.
- `IllegalArgumentException`을 Controller/Service 밖으로 직접 노출하지 않는다.
- 새 도메인 오류가 필요하면 `ErrorCode`에 추가한다.

---

## DTO 규칙

- Request DTO에는 필요한 경우 `@NotBlank`, `@NotNull`, `@Min`, `@Max` 같은 검증 어노테이션을 둔다.
- Response DTO에는 검증 어노테이션을 두지 않는다.
- Request DTO는 `toEntity()`를 둘 수 있다.
- Response DTO는 `from()` 또는 `of()` 정적 팩토리 메서드를 사용한다.
- 프론트와 맞춘 API 필드명은 camelCase를 기본으로 한다.
- DB 저장 형태가 JSON 문자열이어도 공개 API 응답에서는 가능한 한 JSON object/array로 반환한다.

---

## 트랜잭션 규칙

- 쓰기 작업 Service 메서드는 `@Transactional`을 사용한다.
- 읽기 전용 조회 Service 메서드는 `@Transactional(readOnly = true)`를 우선 사용한다.
- Controller에는 트랜잭션을 두지 않는다.
- 외부 API 호출과 DB 저장이 함께 있는 경우 실패 시 재시도, 부분 저장, 중복 저장 가능성을 검토한다.

---

## DB 변경 규칙

- Entity 변경이 테이블 구조 변경을 유발하면 PR 설명에 DB 영향 범위를 명시한다.
- DB 마이그레이션 도구가 확정되기 전까지는 테이블 변경 SQL, 적용 순서, 롤백 방법을 PR 또는 이슈에 명시한다.
- DB 마이그레이션 도구를 도입하면 모든 스키마 변경은 마이그레이션 파일로 관리한다.
- 운영 데이터가 있는 테이블은 삭제보다 숨김, 비활성화, soft delete를 우선 검토한다.
- 대량 조회가 예상되는 필터 조건은 Repository/DB 레벨에서 처리한다.
- 검색/정렬에 자주 쓰이는 필드는 JSON 내부에만 두지 않고 컬럼 승격을 검토한다.
- `createdAt`, `updatedAt` 같은 운영 추적 필드는 도메인 특성에 맞게 일관되게 둔다.

---

## 보안 규칙

- 공개 API는 `SecurityConfig`에서 명시적으로 permitAll 처리한다.
- 공개 허용은 경로뿐 아니라 HTTP Method 기준까지 함께 검토한다.
- 집계 실행, 관리자 수정, 데이터 생성/수정/삭제 API는 공개 허용하지 않는다.
- 관리자 API는 `/api/admin/**` 아래에 둔다.
- 현재 관리자 API는 `X-Admin-Token` 헤더 기반 인증을 사용한다.
- 추후 인증 체계가 안정화되면 `ROLE_ADMIN` 기반 권한으로 전환한다.
- 비밀번호, JWT, 인증 토큰, 외부 API 키는 로그에 남기지 않는다.

---

## 설정과 로그 규칙

- 설정값은 가능한 한 `@ConfigurationProperties`로 묶는다.
- `@Value` 직접 주입은 지양한다.
- `System.out.println`은 사용하지 않고 Log4j2 Logger를 사용한다.
- 외부 API 호출 실패, rate limit, 배치 집계 결과는 운영자가 이해할 수 있는 수준으로 로그를 남긴다.

---

## 외부 데이터 연동 규칙

- Riot API 응답은 백엔드에서 필요한 형태로 가공한 뒤 프론트에 내려준다.
- Community Dragon은 정적 데이터와 이미지 식별자 기준으로 활용한다.
- 프론트가 이미지 URL을 만들 수 있는 경우 DB에는 원본 URL보다 `championKey`, `itemKey`, `traitKey`, `augmentKey` 같은 식별자 저장을 우선 검토한다.
- 대량 데이터 조회/정제는 프론트가 아니라 백엔드 또는 관리자 큐레이션 레이어에서 처리한다.

---

## 테스트 규칙

- 신규 Service 로직에는 Service 레이어 단위 테스트를 우선 작성한다.
- 테스트는 실제 DB나 외부 API에 연결하지 않고 Mock 기반으로 작성한다.
- 기본 패턴은 `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`를 사용한다.
- 테스트 시나리오는 given/when/then 흐름으로 작성한다.
- API 계약이 바뀌면 Controller 테스트 또는 DTO 매핑 테스트를 추가한다.

---

## PR 규칙

- 백엔드 작업도 1 PR = 1 기능 단위를 권장한다.
- Controller, Service, Repository, DTO, Entity가 함께 바뀌는 경우 PR 설명에 API 계약과 DB 변경 여부를 명시한다.
- 공개 API 경로가 바뀌면 프론트 담당자에게 공유한다.
- 관리자 API가 추가되면 필요한 헤더, 권한, 삭제/숨김 정책을 PR 설명에 포함한다.
