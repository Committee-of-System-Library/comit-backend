# AGENTS.md — Comit 프로젝트 AI 컨텍스트

AI 에이전트(Claude, Cursor, Copilot 등)가 이 프로젝트에서 일관된 컨텍스트를 유지하기 위한 공통 문서.
코드 작성, 리뷰, 설계 제안 전에 반드시 읽어야 할 내용.

---

## 프로젝트 개요

**Comit** — 경북대학교 컴퓨터학부 인증 기반 커뮤니티 플랫폼 백엔드
- 그룹: `kr.ac.knu`
- 패키지 루트: `kr.ac.knu.comit`

> **왜 만드는지, 누구를 위한 것인지 → [`docs/features/comit-cps.md`](docs/features/comit-cps.md)**
> 코드 기여, 기능 판단, 우선순위 결정 전에 반드시 읽을 것.

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| ORM | Spring Data JPA |
| DB | MySQL + Flyway (마이그레이션) |
| Validation | spring-boot-starter-validation |
| Lombok | 사용 |
| Test | JUnit 5 + Testcontainers (MySQL) |

---

## 패키지 구조 원칙

도메인 우선 패키지 분리 (Light DDD). 도메인 안에서 레이어드 구조 유지.
팀원이 도메인 단위로 작업을 나눌 수 있어 파일 충돌 최소화.

```text
kr.ac.knu.comit/
├── global/               # 공통 예외, 응답 형식 등
│   └── exception/
├── {도메인}/             # post / user / comment / notice / event ...
│   ├── controller/
│   │   ├── api/          # @ApiContract 인터페이스 (문서 + 라우팅 정의)
│   │   └── {Domain}Controller.java
│   ├── service/          # 비즈니스 로직, 트랜잭션 경계
│   ├── domain/           # Entity + Repository (도메인 응집)
│   │   ├── {Domain}.java
│   │   └── {Domain}Repository.java
│   └── dto/              # Request / Response DTO
└── docs/                 # API 문서 자동 생성기
    └── ApiDocGenerator.java
```

---

## 코드 컨벤션

### 컨트롤러
- 문서·라우팅 정의 → `controller/api/{Domain}ControllerApi.java` (`@ApiContract` 인터페이스)
- 비즈니스 로직 → `{Domain}Controller.java` (`implements`만 선언, 로직 없음)
- `@ApiDoc` 어노테이션은 인터페이스에만 작성
- 응답은 `ResponseEntity<T>` 통일

### DTO 네이밍
- `{행위}{도메인}Request` / `{행위}{도메인}Response` 형식
- 예: `PaymentConfirmRequest`, `PaymentDetailResponse`
- 행위가 다르면 반드시 별도 DTO (같은 필드여도 재사용 금지)

### 트랜잭션
- 클래스에 `@Transactional(readOnly = true)` 기본 적용
- 쓰기(INSERT/UPDATE/DELETE) 메서드에만 `@Transactional` 명시
- Controller / Repository에는 트랜잭션 어노테이션 금지

### 예외
- 예외는 `global/exception/` 하위에서 공통 처리
- 비즈니스 예외는 `BusinessException` + 도메인별 `ErrorCode` (`CommonErrorCode`, `MemberErrorCode`, `PostErrorCode`, `CommentErrorCode`)로 관리
- 외부 에러 응답은 `ProblemDetail` 표준을 사용하며 세부 포맷은 `docs/adr/002-problem-detail-error-response.md`를 따른다.

### Entity 설계
- `@NoArgsConstructor(access = PROTECTED)` 필수 — 외부 직접 생성 금지
- 정적 팩토리 메서드 `create(...)` 로 생성 — 생성 규칙·검증 캡슐화
- setter 금지 — 상태 변경은 의미 있는 메서드명 사용 (`post.update(...)`, `post.delete()`)
- soft delete: `deletedAt` 필드 통일 (`LocalDateTime`, nullable)
- 도메인 규칙 검증(null, blank, 길이 등)은 Entity 안에서 `private static void validateXxx()`

### Service 설계
- Controller가 직접 호출하는 메서드만 `public` — 내부 헬퍼는 `private`
- 내부 조회 헬퍼 네이밍: `findXxxOrThrow(id)` — 없으면 `BusinessException` throw
- 타 도메인 Repository 직접 접근 금지 → 해당 도메인의 Service 메서드 호출
- 비즈니스 권한 검증(소유자 확인, 중복 등)은 Service에서 수행

### DTO 변환 책임
- **Request DTO**: 순수 데이터 운반만. Entity/변환 로직 없음
- **Response DTO**: `from(Entity)` 정적 팩토리 메서드 허용 — Entity → DTO 변환은 DTO 책임
- Request → Entity 변환: Service에서 직접 수행 (`Entity.create(request.field(), ...)`)
- MapStruct/ModelMapper 사용 금지 — 명시적 변환 원칙

### Validation 책임 분리
| 검증 유형 | 위치 |
|---|---|
| 형식 (null, blank, 길이) | Request DTO의 Bean Validation (`@NotBlank`, `@Size`) + Entity 내부 |
| 도메인 규칙 (값의 의미) | Entity 정적 팩토리 / 상태 변경 메서드 |
| 비즈니스 규칙 (중복, 권한) | Service |

---

## API 문서 자동화

- `@ApiContract` 인터페이스 스캔 → `docs/api/` 하위 HTML 자동 생성
- 생성 명령: `./gradlew generateApiDocs`
- 컨트롤러 변경 시 반드시 문서 재생성 후 커밋
- CI에서 `git diff docs/api/` 검증 — 문서 미업데이트 시 PR 통과 불가
- `main` 브랜치 push 시 GitHub Pages workflow가 `docs/api/`를 정적 사이트로 배포한다.

---

## 문서 동기화 순서

코드 변경이 생기면 문서는 아래 순서로 함께 갱신한다.

1. 변경의 성격을 먼저 분류한다.
   기능 맥락 변경 / 기술 결정 변경 / API 계약 변경 / 문서 생성기 변경 / 테스트 전략 변경 / 운영 변경
2. 기준 문서를 먼저 갱신한다.
   기능 방향은 `docs/features/`, 기술 결정은 `docs/adr/`를 우선 수정한다.
3. 개발 가이드를 갱신한다.
   API 계약은 `docs/guides/api-contract.md`, 생성기 동작은 `docs/guides/api-doc-generator-flow.md`, 테스트 원칙은 `docs/guides/testing-strategy.md`를 본다.
4. 산출물을 재생성한다.
   API 계약이나 생성기 관련 변경이면 `./gradlew generateApiDocs`를 실행해 `docs/api/`를 갱신한다.
5. 인덱스 문서를 마지막에 맞춘다.
   새 문서가 생기거나 문서 성격이 바뀌면 `docs/README.md`, 각 하위 폴더의 `README.md`를 함께 갱신한다.

### 변경 유형별로 같이 봐야 하는 문서

| 변경 유형 | 반드시 검토할 문서 |
|---|---|
| 제품 방향, 우선순위, 왜 만드는지 | `docs/features/comit-cps.md`, `docs/features/README.md`, `docs/README.md` |
| 기능 설계, 예외 케이스, 도메인 흐름 | 해당 `docs/features/*.md`, `docs/features/README.md`, 필요 시 관련 ADR |
| 에러 응답 포맷, 에러 코드 체계 | `docs/adr/002-problem-detail-error-response.md`, `docs/guides/api-contract.md`, `docs/api/` |
| 컨트롤러 계약, DTO, validation, 인증 주입 | `docs/guides/api-contract.md`, `docs/api/` |
| API 문서 생성기 로직, 어노테이션 규칙 | `docs/guides/api-doc-generator-flow.md`, `docs/api/`, `docs/README.md` |
| 테스트 전략, 테스트 레벨, 네이밍 | `docs/guides/testing-strategy.md`, `docs/guides/README.md`, `docs/README.md` |
| 로컬 실행, 배포, 환경 변수, 운영 절차 | `docs/ops/` 하위 문서와 해당 `README.md` |

---

## 브랜치 / 커밋 컨벤션

- 브랜치: `feat/#이슈번호`, `fix/#이슈번호`, `refactor/#이슈번호`
- 커밋: Conventional Commits 형식
  - `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`
- 상세: `docs/guides/git-convention.md`

---

## 테스트 원칙

- DB 관련 테스트는 Testcontainers 사용 (Mock DB 금지)
- 통합 테스트는 실제 MySQL 컨테이너로 검증

---

## docs 구조

| 폴더 | 목적 |
|---|---|
| `docs/adr/` | 왜 이런 결정을 했는지 (Architecture Decision Records) |
| `docs/guides/` | 개발자가 따라야 하는 작성법·컨벤션·사용법 |
| `docs/features/` | 기능별 설계·흐름·예외 케이스 |
| `docs/ops/` | 배포·환경변수·운영 체크리스트·장애 대응 |
| `docs/api/` | `generateApiDocs` 자동 생성 HTML 산출물 |

---

## ADR 목록

| 번호 | 제목 | 상태 |
|---|---|---|
| ADR-001 | API 문서 자동화 방식 선택 | 채택 |
| ADR-002 | ProblemDetail 기반 에러 응답 표준 채택 | 채택 |
