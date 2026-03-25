# ADR-002. ProblemDetail 기반 에러 응답 표준 채택

## 상태: 채택

## 날짜: 2026-03-24

---

## 맥락

- 현재 에러 응답은 `ApiResponse.error(...)` 중심이라 RFC 9457 표준과 거리가 있다.
- 도메인별 에러가 늘어나면서 프론트엔드가 일관된 방식으로 분기할 수 있는 구조가 필요하다.
- validation 에러, 비즈니스 에러, 시스템 에러를 서로 다른 형태로 내려보내면 클라이언트 처리 비용이 커진다.
- 서버 내부 예외 메시지와 스택 트레이스는 외부에 노출하지 않고, 로그 추적에 필요한 최소 정보만 전달해야 한다.

---

## 결정

Spring Framework 6 / Spring Boot 3부터 제공되는 `ProblemDetail`을 기준으로 에러 응답 표준을 통일한다.

### 기본 원칙

- 에러 응답은 RFC 9457 기본 필드 `type`, `title`, `status`, `detail`, `instance`를 유지한다.
- `type`은 배포 환경에 종속되지 않도록 `/problems/...` 형태의 상대 경로 URI를 사용한다.
- 프론트엔드 분기와 로그 검색을 위해 `errorCode`를 확장 필드로 추가한다.
- validation 에러는 `invalidFields[]` 배열로 필드 단위 정보를 노출한다.
- 시스템 에러는 `errorTrackingId`를 포함해 서버 로그와 연결한다.
- 성공 응답은 당장 바꾸지 않고 기존 DTO 또는 success envelope 구조를 유지한다.

### 목표 구조

```text
global/exception/
├── ErrorCode.java
├── CommonErrorCode.java
├── MemberErrorCode.java
├── PostErrorCode.java
├── CommentErrorCode.java
├── BusinessException.java
├── ProblemDetailFactory.java
├── ProblemFieldViolation.java
└── GlobalExceptionHandler.java
```

### 예외 흐름

```text
controller
  -> service
  -> BusinessException(ErrorCode)
  -> GlobalExceptionHandler
  -> ProblemDetailFactory
  -> response json
```

1. Controller는 입력을 받고 Service를 호출한다.
2. Service는 비즈니스 규칙 위반 시 `BusinessException`만 던진다.
3. `BusinessException`은 반드시 `ErrorCode`를 가진다.
4. `GlobalExceptionHandler`는 예외 종류에 따라 `ProblemDetailFactory`를 호출한다.
5. `ProblemDetailFactory`는 표준 필드와 `errorCode`, `invalidFields`, `errorTrackingId` 같은 확장 필드를 채운다.
6. Controller 밖으로는 항상 표준화된 에러 JSON만 나간다.

### 응답 예시

비즈니스 에러:

```json
{
  "type": "/problems/member/duplicate-nickname",
  "title": "Conflict",
  "status": 409,
  "detail": "이미 사용 중인 닉네임입니다.",
  "instance": "/members/me",
  "errorCode": "DUPLICATE_NICKNAME",
  "timestamp": "2026-03-24T03:10:00Z"
}
```

Validation 에러:

```json
{
  "type": "/problems/common/invalid-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값이 올바르지 않습니다.",
  "instance": "/posts",
  "errorCode": "INVALID_REQUEST",
  "invalidFields": [
    {
      "field": "title",
      "message": "제목을 입력해주세요."
    }
  ],
  "timestamp": "2026-03-24T03:10:00Z"
}
```

시스템 에러:

```json
{
  "type": "/problems/common/internal-server-error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요.",
  "instance": "/posts/1",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "errorTrackingId": "4a2b99f8-6e5d-4ce9-a86e-2d1f5f1fdb65",
  "timestamp": "2026-03-24T03:10:00Z"
}
```

---

## 검토한 대안

| 대안 | 거절 이유 |
|---|---|
| `ApiResponse.error(...)` 유지 | 표준성이 낮고 에러 유형별 구조 확장이 어렵다. |
| 모든 에러를 단일 enum에 계속 유지 | 도메인 증가 시 코드가 커지고 변경 영향 범위가 넓어진다. |
| 성공/실패 모두 같은 envelope로 통일 | 현재 변경 범위를 불필요하게 키우고 기존 API 영향이 크다. |

---

## 결과

- 프론트엔드는 `type`과 `errorCode`를 기준으로 일관되게 분기할 수 있다.
- validation 에러를 필드 단위로 노출해 화면 매핑이 쉬워진다.
- 500 에러는 외부에 내부 구현을 노출하지 않고도 추적이 가능해진다.
- 에러 코드를 도메인별로 분리할 수 있는 방향이 정해진다.

### 현재 구조와 목표 구조

| 현재 | 목표 |
|---|---|
| `BusinessErrorCode` 하나에 모든 에러 코드 집중 | 도메인별 `*ErrorCode` enum으로 분리 |
| `ApiResponse.error(...)` 기반 응답 | `ProblemDetail` 기반 표준 응답 |
| validation 에러 구조가 단순 문자열 위주 | `invalidFields[]`로 필드 단위 노출 |
| 500 에러에 추적 키 없음 | `errorTrackingId`로 로그와 연결 |

---

## 후속 작업

1. `ErrorCode` 계약을 `ProblemDetail` 기준 필드까지 확장한다. 완료
2. `GlobalExceptionHandler`에서 `ApiResponse.error(...)` 대신 `ProblemDetail`을 반환한다. 완료
3. validation 에러를 `invalidFields` 배열 구조로 표준화한다. 완료
4. 500 에러에 `errorTrackingId`를 추가하고 서버 로그와 연결한다. 완료
5. API 문서 예시를 새 에러 포맷 기준으로 갱신한다. 완료
6. 공통/도메인별 `*ErrorCode` enum 분리를 적용한다. 완료

---

## 남은 결정사항

- 성공 응답을 지금처럼 `ApiResponse`로 유지할지, 성공도 별도 표준 포맷으로 맞출지
- `errorTrackingId`를 MDC 기반으로 만들지, 핸들러에서 UUID를 즉시 생성할지
- 실제 SSO starter 연동 이후 인증/인가 에러 코드를 어느 도메인 enum에 둘지
