# AGENTS.md — Comit 프로젝트 AI 컨텍스트

AI 에이전트(Claude, Cursor, Copilot 등)가 이 프로젝트에서 일관된 컨텍스트를 유지하기 위한 공통 문서.
코드 작성, 리뷰, 설계 제안 전에 반드시 읽어야 할 내용.

---

## 프로젝트 개요

**Comit** — 경북대학교 컴퓨터학부 동아리 서버 백엔드
- 그룹: `kr.ac.knu`
- 패키지 루트: `kr.ac.knu.comit`

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
- 비즈니스 예외는 `BusinessException` + `BusinessErrorCode`로 관리

---

## API 문서 자동화

- `@ApiContract` 인터페이스 스캔 → `docs/api/` 하위 HTML 자동 생성
- 생성 명령: `./gradlew generateApiDocs`
- 컨트롤러 변경 시 반드시 문서 재생성 후 커밋
- CI에서 `git diff docs/api/` 검증 — 문서 미업데이트 시 PR 통과 불가

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
