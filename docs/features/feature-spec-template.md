# Feature Spec Template

새 기능을 구현하기 전, 아래 템플릿을 복사해 기능 명세를 먼저 정리한다.
이 문서는 PRD 대체물이 아니라 `Comit` 코드베이스에서 바로 구현 가능한 수준의 기능 명세를 만드는 데 목적이 있다.

---

# {기능명}

## 1. Overview

### 1.1 Goal
- 무엇을 만드는가
- 왜 필요한가
- 성공하면 사용자/시스템이 무엇을 할 수 있게 되는가

### 1.2 In Scope
- 이번 작업에 포함되는 범위
- 이번 이슈 또는 PR에서 반드시 끝내야 하는 것

### 1.3 Out of Scope
- 이번 작업에서 하지 않는 것
- 후속 작업으로 미루는 것

### 1.4 Success Signal
- 이 기능이 완료되었다고 판단하는 기준
- 예: 특정 시나리오가 동작한다, 특정 응답이 보장된다, 특정 운영 조건을 충족한다

---

## 2. Domain Context

### 2.1 Domain Terms
- 용어 1:
- 용어 2:

### 2.2 Actors
- 사용자:
- 관리자:
- 외부 시스템:

### 2.3 Assumptions
- 이 기능이 성립하기 위한 전제
- 이미 존재한다고 가정하는 시스템, 데이터, 정책
- 명시되지 않으면 안 되는 가정

---

## 3. Scenarios

### Scenario A. {대표 정상 시나리오}
Given:
- 시작 상태
- 필요한 데이터, 권한, 맥락

When:
- 사용자가 어떤 행동을 한다

Then:
- 시스템이 어떤 결과를 반환한다
- 상태가 어떻게 바뀐다

### Scenario B. {실패 또는 예외 시나리오}
Given:
- 실패 조건

When:
- 사용자가 행동하거나 외부 조건이 충족된다

Then:
- 어떤 에러가 발생하는가
- 어떤 상태는 바뀌지 않아야 하는가

### Scenario C. {경계 또는 정책 시나리오}
Given:
When:
Then:

---

## 4. Functional Requirements
- FR-1:
- FR-2:
- FR-3:

작성 규칙:
- 각 요구사항은 검증 가능해야 한다
- “좋아 보이게” 같은 모호한 표현 대신 관찰 가능한 결과로 적는다

---

## 5. Behavioral Rules

### 5.1 Preconditions
- 기능 실행 전에 만족해야 하는 조건
- 예: 로그인 상태, 유효한 토큰, 활성 상태 엔티티

### 5.2 Postconditions
- 기능 실행 후 반드시 성립해야 하는 결과
- 예: 엔티티 생성, 상태 변경, 이벤트 발행, 응답 보장

### 5.3 Invariants
- 항상 유지되어야 하는 규칙
- 예: soft delete 된 데이터는 조회 결과에 포함되지 않는다
- 예: 한 사용자는 같은 대상에 중복 반응할 수 없다

### 5.4 Forbidden Rules
- 절대 허용되면 안 되는 동작
- 예: 다른 게시글의 댓글을 부모 댓글로 지정할 수 없다
- 예: 권한 없는 사용자가 관리자 API를 호출할 수 없다

---

## 6. State Model

### 6.1 States
- 상태 A:
- 상태 B:

### 6.2 Transition Rules
- A -> B:
- B -> C:

### 6.3 Invalid Transitions
- 허용되지 않는 상태 전이
- 예: 삭제됨 -> 수정됨

필요하면 표나 mermaid 다이어그램을 추가한다.

---

## 7. External Contracts

### 7.1 API Contract
- Endpoint:
- Method:
- Request:
- Response:
- Error cases:

### 7.2 Integration Contract
- 외부 시스템:
- 입력/출력:
- callback, redirect, event 규칙:
- 실패 시 처리 방식:

### 7.3 Persistence Contract
- 어떤 데이터가 저장되는가
- 어떤 필드가 변경되는가
- 유니크, 정합성 제약은 무엇인가

---

## 8. Non-Functional Constraints

### 8.1 Technical Constraints
- 기술 스택:
- 아키텍처 제약:
- 코드 규칙:

### 8.2 Security Constraints
- 인증:
- 권한:
- 민감정보 처리:
- 신뢰하면 안 되는 입력:

### 8.3 Performance Constraints
- 응답 시간:
- 쿼리 수:
- 캐시, 배치, 페이지네이션 요구:

### 8.4 Observability Constraints
- 로그:
- 메트릭:
- 추적, 감사 필요 여부:

---

## 9. Test Criteria

### 9.1 Happy Path
- 정상 케이스 1
- 정상 케이스 2

### 9.2 Failure Cases
- 인증 실패
- 권한 실패
- 유효성 검증 실패
- 정합성 위반

### 9.3 Edge Cases
- 빈 값
- 중복 요청
- 동시성
- 마지막 페이지, 경계 길이, 만료 시점

### 9.4 Regression Risks
- 기존 어떤 기능이 깨질 수 있는가
- 반드시 같이 확인해야 하는 흐름은 무엇인가

---

## 10. Open Questions
- 아직 결정되지 않은 사항
- 구현 전에 답이 필요한 항목
- 추측하면 안 되는 부분

---

## 11. Implementation Guardrails
- 추측 금지
- 테스트 포함
- 계층 분리 유지
- DTO / Entity 분리
- 계약을 바꾸면 문서와 테스트를 함께 갱신

---

## 작성 팁
- 기능이 작아도 최소 `Goal`, `Scenarios`, `Functional Requirements`, `Test Criteria`는 채운다.
- 복잡한 기능이면 `State Model`, `Integration Contract`, `Persistence Contract`를 생략하지 않는다.
- 구현 규칙이 팀 공통 규칙과 충돌하면 [AGENTS.md](../../AGENTS.md) 와 `docs/guides/` 문서를 우선 확인한다.
