# 게시글·댓글 신고 기능

## 1. Overview

### 1.1 Goal
- 게시글과 댓글에 대해 사용자가 신고를 남길 수 있어야 한다.
- 부적절한 콘텐츠를 운영자가 사후 확인할 수 있는 최소 안전장치를 만든다.
- 초기 버전은 신고 접수와 저장에 집중하고, 운영 처리 UI는 후속 작업으로 분리한다.

### 1.2 In Scope
- 게시글 신고 API
- 댓글 신고 API
- 신고 사유 메시지 입력
- 신고 데이터 저장
- 동일 사용자의 동일 대상 중복 신고 방지
- 기본 validation, 에러 처리, 테스트

### 1.3 Out of Scope
- 관리자 신고 목록 조회 UI/API
- 신고 처리 상태 변경 UI/API
- 자동 차단, 자동 숨김
- 신고 카테고리 분류
- 신고 대상자 알림

### 1.4 Success Signal
- 로그인 사용자가 게시글과 댓글을 신고할 수 있다.
- 신고 사유는 `1~500자`로 검증된다.
- 같은 사용자가 같은 게시글 또는 댓글을 반복 신고할 수 없다.
- 삭제된 대상이나 존재하지 않는 대상은 신고할 수 없다.

---

## 2. Domain Context

### 2.1 Domain Terms
- `Report`: 사용자가 특정 게시글 또는 댓글에 대해 남긴 신고 기록
- `TargetType`: 신고 대상 유형 (`POST`, `COMMENT`)
- `ReasonMessage`: 신고 사유 자유 입력 메시지
- `ReportStatus`: 신고 접수 및 후속 검토 상태

### 2.2 Actors
- 사용자: 게시글/댓글을 신고하는 회원
- 관리자: 후속 단계에서 신고를 검토하는 운영자
- 외부 시스템: 없음

### 2.3 Assumptions
- 신고는 로그인 사용자만 가능하다.
- 신고 대상은 활성 게시글 또는 활성 댓글만 허용한다.
- v1에서는 운영 검토용 저장만 하고, 처리 상태는 일반 사용자에게 노출하지 않는다.
- 신고 사유는 자유 입력 메시지 하나만 받으며, 카테고리 선택은 후속 작업으로 미룬다.

---

## 3. Scenarios

### Scenario A. 게시글 신고를 접수한다
Given:
- 로그인 사용자가 존재한다.
- 신고 대상 게시글이 활성 상태다.
- 사용자가 1자 이상 500자 이하의 신고 메시지를 입력했다.

When:
- 사용자가 게시글 신고를 요청한다.

Then:
- 게시글 대상 신고 레코드가 저장된다.
- 생성된 `reportId`가 반환된다.

### Scenario B. 댓글 신고를 접수한다
Given:
- 로그인 사용자가 존재한다.
- 신고 대상 댓글이 활성 상태다.
- 사용자가 1자 이상 500자 이하의 신고 메시지를 입력했다.

When:
- 사용자가 댓글 신고를 요청한다.

Then:
- 댓글 대상 신고 레코드가 저장된다.
- 생성된 `reportId`가 반환된다.

### Scenario C. 중복 또는 잘못된 신고를 거부한다
Given:
- 같은 사용자가 같은 게시글 또는 댓글을 이미 신고했거나,
- 대상이 삭제되었거나 존재하지 않거나,
- 메시지가 공백이거나 500자를 초과한다.

When:
- 사용자가 신고를 요청한다.

Then:
- 중복이면 `409 CONFLICT`가 반환된다.
- 대상이 없거나 삭제되었으면 대상별 not found 에러가 반환된다.
- 메시지가 유효하지 않으면 `400 INVALID_REQUEST`가 반환된다.
- 새로운 신고 레코드는 저장되지 않는다.

---

## 4. Functional Requirements
- FR-1: 사용자는 게시글을 신고할 수 있어야 한다.
- FR-2: 사용자는 댓글을 신고할 수 있어야 한다.
- FR-3: 신고 요청은 `message` 필드 하나를 받는다.
- FR-4: `message`는 공백만 입력할 수 없고 최대 500자까지 허용한다.
- FR-5: 같은 사용자는 같은 대상(`targetType + targetId`)을 한 번만 신고할 수 있다.
- FR-6: 성공 시 생성된 신고 ID를 반환한다.
- FR-7: v1 신고는 접수 상태로만 생성되고, 상태 변경 기능은 포함하지 않는다.

---

## 5. Behavioral Rules

### 5.1 Preconditions
- 사용자 인증 완료
- 대상 게시글/댓글이 활성 상태
- 메시지가 비어 있지 않음
- 메시지 길이가 500자 이하

### 5.2 Postconditions
- 신고 레코드 1건 생성
- `createdAt` 기록
- 초기 상태는 `RECEIVED`
- 동일 사용자/동일 대상 중복 신고는 이후 차단

### 5.3 Invariants
- soft delete 된 게시글/댓글은 신고 대상이 될 수 없다.
- 신고 사유 원문은 trim 후 검증한다.
- 신고 대상 타입은 `POST` 또는 `COMMENT`만 허용한다.
- 동일 사용자와 동일 대상 조합은 DB 레벨에서도 유일해야 한다.

### 5.4 Forbidden Rules
- 비로그인 사용자의 신고
- 공백 메시지 신고
- 500자 초과 메시지 신고
- 같은 사용자의 동일 대상 중복 신고
- 다른 도메인 대상에 대한 임의 신고 타입 확장

---

## 6. State Model

### 6.1 States
- `RECEIVED`: 접수됨
- `REVIEWED`: 운영자가 확인함
- `DISMISSED`: 조치 없이 종료
- `ACTIONED`: 실제 조치 완료

### 6.2 Transition Rules
- v1 구현 범위는 `RECEIVED` 생성까지만 포함한다.
- 후속 운영 기능이 추가되면 `RECEIVED -> REVIEWED | DISMISSED | ACTIONED`를 허용한다.

### 6.3 Invalid Transitions
- v1에서는 생성 후 상태 변경이 없다.
- 존재하지 않는 신고 상태로 직접 전이할 수 없다.

필요 시 후속 단계에서 운영 상태 전이 다이어그램을 추가한다.

---

## 7. External Contracts

### 7.1 API Contract
- Endpoint: `POST /posts/{postId}/reports`
  - Request:
    ```json
    {
      "message": "광고성 도배입니다"
    }
    ```
  - Response:
    ```json
    {
      "reportId": 123
    }
    ```

- Endpoint: `POST /comments/{commentId}/reports`
  - Request:
    ```json
    {
      "message": "욕설이 포함되어 있습니다"
    }
    ```
  - Response:
    ```json
    {
      "reportId": 124
    }
    ```

Error cases:
- `401 UNAUTHORIZED`
- `400 INVALID_REQUEST`
- `404 POST_NOT_FOUND`
- `404 COMMENT_NOT_FOUND`
- `409 REPORT_ALREADY_EXISTS`

### 7.2 Integration Contract
- 외부 연동 없음

### 7.3 Persistence Contract
- v1은 단일 `report` 테이블을 권장한다.
- 필드 예시:
  - `id`
  - `reporter_id`
  - `target_type`
  - `target_id`
  - `message`
  - `status`
  - `created_at`
  - `reviewed_at` nullable
  - `reviewed_by` nullable
- 유니크 제약:
  - `(reporter_id, target_type, target_id)`

---

## 8. Non-Functional Constraints

### 8.1 Technical Constraints
- 도메인 패키지는 `report`로 분리한다.
- DTO와 Entity 책임을 분리한다.
- 컨트롤러는 API 계약만, 비즈니스 로직은 서비스에 둔다.

### 8.2 Security Constraints
- 신고는 로그인 사용자만 가능하다.
- 신고 사유는 사용자 입력이므로 신뢰하지 않는다.
- 관리자 기능 추가 전까지 신고 목록 공개 API를 만들지 않는다.

### 8.3 Performance Constraints
- 신고 생성은 대상 존재 확인 + insert 수준으로 끝나야 한다.
- 중복 신고 방지는 유니크 제약 또는 그에 준하는 DB 보장으로 처리한다.

### 8.4 Observability Constraints
- 중복 신고와 validation 실패는 문제 추적이 가능하도록 로그가 남아야 한다.
- 후속 운영 확장을 고려해 상태 필드는 초기에 포함한다.

---

## 9. Test Criteria

### 9.1 Happy Path
- 게시글 신고 성공
- 댓글 신고 성공

### 9.2 Failure Cases
- 비로그인 사용자 신고 실패
- 존재하지 않는 게시글 신고 실패
- 존재하지 않는 댓글 신고 실패
- 공백 메시지 신고 실패
- 500자 초과 메시지 신고 실패
- 중복 신고 실패

### 9.3 Edge Cases
- trim 후 빈 문자열
- 정확히 500자 메시지
- soft delete 된 게시글/댓글 신고
- 대댓글 신고

### 9.4 Regression Risks
- 기존 게시글/댓글 조회/삭제 로직과 soft delete 기준이 어긋나지 않는지
- 게시글/댓글 도메인에 신고 저장 책임이 과도하게 새지 않는지

---

## 10. Open Questions
- 신고 접수 즉시 숨김 처리까지 할지
- 관리자 검토 API를 Phase 1에 넣을지
- 신고 카테고리를 v1부터 넣을지, 자유 입력만 유지할지

---

## 11. Implementation Guardrails
- 추측 금지
- 테스트 포함
- `message` 제한은 DTO와 Entity 둘 다 검증한다.
- 계약이 바뀌면 API 문서와 feature 문서를 함께 갱신한다.
- 게시글과 댓글 신고는 공통 정책을 가지되, 대상 조회와 not found 에러는 각 도메인 규칙을 따른다.
