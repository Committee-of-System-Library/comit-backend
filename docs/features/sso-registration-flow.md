# SSO 미가입 회원 2단계 회원가입 플로우

## 1. Overview

### 1.1 Goal
- SSO 로그인 성공 후 Comit 미가입 사용자가 명시적으로 정보를 입력하고 회원가입을 완료할 수 있어야 한다.
- 현재 자동 생성(`findOrCreateBySso`) 정책을 제거하고 사용자 주도 등록으로 전환한다.
- 회원가입 완료 전까지는 일반 API 접근을 차단한다.

### 1.2 In Scope
- `SsoCallbackPendingRegistration` 결과 타입 추가
- `GET /auth/register/prefill` — JWT에서 pre-fill 가능한 값 반환
- `POST /auth/register` — 회원가입 완료 API
- `Member` 도메인 필드 확장 (`name`, `phone`, `majorTrack`, `agreedAt`)
- `ExternalIdentity` `major` 필드 추가
- `SsoAuthenticationFilter` 미가입 상태 접근 차단
- `MemberRegistrationService` auto-create 제거

### 1.3 Out of Scope
- 프로필 이미지
- 약관 버전 관리
- 회원가입 후 이메일 인증

### 1.4 Success Signal
- 미가입 INTERNAL 사용자가 SSO 콜백 후 register URL로 리디렉션된다.
- `GET /auth/register/prefill`에서 name, studentNumber, major가 반환된다.
- `POST /auth/register`로 회원가입 완료 후 정상 서비스 진입이 된다.
- 미가입 상태에서 `/auth/register/**` 외 API 접근 시 `REGISTRATION_REQUIRED` 에러가 반환된다.
- 기존 가입 사용자의 로그인 플로우는 그대로 동작한다.

---

## 2. Domain Context

### 2.1 Domain Terms
- `PendingRegistration`: SSO 인증은 통과했으나 Comit 회원 등록이 완료되지 않은 상태
- `prefill`: JWT claim에서 서버가 추출하여 프론트에 제공하는 사전 입력값
- `agreedAt`: 개인정보 수집 및 서비스 이용약관 동의 시각

### 2.2 Actors
- 미가입 INTERNAL 사용자: SSO 인증은 완료했으나 Comit 회원이 아닌 상태
- 기가입 사용자: 기존 플로우 그대로
- EXTERNAL 사용자: 기존과 동일하게 차단

### 2.3 Assumptions
- SSO JWT에 `major` claim이 포함되어 있다.
- `name`, `studentNumber`, `major`는 JWT를 신뢰하며 서버가 직접 읽는다. 프론트에서 전달받지 않는다.
- `name`은 수정 불가 (실명 고정).
- 회원가입 완료 전 SSO 토큰 쿠키는 이미 브라우저에 심겨 있다.
- `ComitSsoProperties`에 `frontendRegisterUrl`이 추가된다.

---

## 3. Scenarios

### Scenario A. 미가입 INTERNAL 사용자가 SSO 콜백 후 회원가입 페이지로 이동한다
Given:
- INTERNAL user_type을 가진 SSO 사용자가 콜백 토큰을 전달했다.
- DB에 해당 ssoSub의 멤버가 없다.

When:
- `/auth/sso/callback`이 호출된다.

Then:
- SSO 토큰 쿠키가 세팅된다.
- `frontendRegisterUrl`로 302 리디렉션된다.

### Scenario B. 프론트가 prefill 값을 조회한다
Given:
- SSO 토큰 쿠키가 세팅되어 있다.
- DB에 해당 ssoSub의 멤버가 없다.

When:
- `GET /auth/register/prefill`을 호출한다.

Then:
- JWT에서 `name`, `studentNumber`, `major`를 추출하여 반환한다.

### Scenario C. 사용자가 회원가입을 완료한다
Given:
- SSO 토큰 쿠키가 세팅되어 있다.
- DB에 해당 ssoSub의 멤버가 없다.
- `nickname`, `phone`, `agreedToTerms: true`를 전달한다.

When:
- `POST /auth/register`를 호출한다.

Then:
- Member가 생성된다. (`name`, `phone`, `nickname`, `studentNumber`, `majorTrack`, `agreedAt` 저장)
- `200 OK`가 반환된다.
- 이후 일반 API 접근이 가능해진다.

### Scenario D. 미가입 상태에서 일반 API에 접근한다
Given:
- SSO 토큰 쿠키가 세팅되어 있다.
- DB에 해당 ssoSub의 멤버가 없다.

When:
- `/auth/register/**` 외 API를 호출한다.

Then:
- `403 REGISTRATION_REQUIRED` 에러가 반환된다.

### Scenario E. 기존 가입 사용자가 로그인한다
Given:
- DB에 해당 ssoSub의 멤버가 존재한다.

When:
- `/auth/sso/callback`이 호출된다.

Then:
- 기존과 동일하게 `frontendSuccessUrl`로 302 리디렉션된다.

### Scenario F. 이미 가입한 사용자가 register API를 호출한다
Given:
- SSO 토큰 쿠키가 세팅되어 있다.
- DB에 해당 ssoSub의 멤버가 이미 존재한다.

When:
- `POST /auth/register`를 호출한다.

Then:
- `409 MEMBER_ALREADY_EXISTS` 에러가 반환된다.

---

## 4. Functional Requirements
- FR-1: SSO 콜백 시 INTERNAL + 미가입이면 `SsoCallbackPendingRegistration`을 반환하고 register URL로 리디렉션한다.
- FR-2: `GET /auth/register/prefill`은 SSO 쿠키를 검증하고 JWT에서 `name`, `studentNumber`, `major`를 반환한다.
- FR-3: `POST /auth/register`는 `nickname`, `phone`, `agreedToTerms`를 받는다.
- FR-4: `agreedToTerms`가 `false`이면 `400 INVALID_REQUEST`를 반환한다.
- FR-5: `nickname`은 1~15자, 중복 불가.
- FR-6: `phone`은 필수 입력값이다.
- FR-7: 미가입 상태에서 `/auth/register/**` 외 접근 시 `REGISTRATION_REQUIRED` 에러를 반환한다.
- FR-8: 회원가입 완료 후 이미 존재하는 ssoSub으로 재등록 시도 시 `MEMBER_ALREADY_EXISTS`를 반환한다.
- FR-9: `MemberRegistrationService`의 자동 생성(auto-create) 로직을 제거한다.

---

## 5. Behavioral Rules

### 5.1 Preconditions
- `GET /auth/register/prefill`, `POST /auth/register` 모두 유효한 SSO 토큰 쿠키 필요
- `agreedToTerms == true`

### 5.2 Postconditions
- Member 생성: `name`, `phone`, `nickname`, `studentNumber`, `majorTrack`, `agreedAt`, `ssoSub` 저장
- `agreedAt = 서버 현재 시각`
- 이후 `SsoAuthenticationFilter`에서 정상 멤버로 인식

### 5.3 Invariants
- `name`, `studentNumber`, `major`는 서버가 JWT에서 직접 읽는다. 프론트 전달값을 신뢰하지 않는다.
- 한 ssoSub에 하나의 멤버만 존재한다.
- `agreedToTerms == false`이면 회원가입이 완료되지 않는다.

### 5.4 Forbidden Rules
- 프론트에서 `name`, `studentNumber`, `major`를 전달받아 저장하는 것
- 미가입 상태에서 일반 API 접근 허용
- auto-create로 닉네임을 자동 생성하는 것

---

## 6. State Model

### 6.1 SSO 콜백 결과 상태
- `SsoCallbackSuccess`: 기가입 INTERNAL → success URL
- `SsoCallbackRejected`: EXTERNAL → error URL
- `SsoCallbackPendingRegistration`: 미가입 INTERNAL → register URL (신규)

### 6.2 멤버 인증 필터 상태
- 쿠키 없음 → anonymous
- 쿠키 있음 + 멤버 있음 → authenticated
- 쿠키 있음 + 멤버 없음 → pending registration → `/auth/register/**` 외 차단

---

## 7. External Contracts

### 7.1 API Contract

**GET /auth/register/prefill**
- Auth: SSO 토큰 쿠키 필요
- Response:
```json
{
  "name": "홍길동",
  "studentNumber": "2023000001",
  "major": "심화"
}
```
- Errors:
  - `401 UNAUTHORIZED`: 쿠키 없음 또는 만료
  - `409 MEMBER_ALREADY_EXISTS`: 이미 가입된 ssoSub

**POST /auth/register**
- Auth: SSO 토큰 쿠키 필요
- Request:
```json
{
  "nickname": "길동이",
  "phone": "010-1234-5678",
  "agreedToTerms": true
}
```
- Response: `200 OK`
- Errors:
  - `400 INVALID_REQUEST`: agreedToTerms == false, nickname 유효성 실패
  - `401 UNAUTHORIZED`: 쿠키 없음
  - `409 MEMBER_ALREADY_EXISTS`: 이미 가입된 ssoSub
  - `409 DUPLICATE_NICKNAME`: 닉네임 중복

### 7.2 Persistence Contract
- Member 테이블 신규 컬럼:
  - `name` VARCHAR NOT NULL
  - `phone` VARCHAR NOT NULL
  - `major_track` VARCHAR NULLABLE (student_number처럼 nullable 허용)
  - `agreed_at` DATETIME NOT NULL

---

## 8. Non-Functional Constraints

### 8.1 Security Constraints
- `name`, `studentNumber`, `major`는 JWT에서만 읽는다.
- register API는 SSO 쿠키 검증 없이 접근 불가.

### 8.2 Technical Constraints
- 기존 `Member.create()` 시그니처 변경 필요 → `MemberFixture` 업데이트 필요
- Flyway 마이그레이션 스크립트 추가 필요

---

## 9. Test Criteria

### 9.1 Happy Path
- 미가입 INTERNAL 콜백 → register URL 리디렉션
- prefill 조회 성공
- 회원가입 완료 → 멤버 생성 확인
- 기가입 사용자 콜백 → success URL 리디렉션 (회귀)

### 9.2 Failure Cases
- `agreedToTerms == false` → 400
- 닉네임 15자 초과 → 400
- 닉네임 중복 → 409
- 이미 가입된 ssoSub으로 재등록 → 409
- 미가입 상태에서 `/posts` 접근 → 403

### 9.3 Edge Cases
- 쿠키 만료 후 prefill 호출
- 회원가입 중 닉네임 중복 race condition
- `studentNumber`가 JWT에 없는 경우 (nullable)
- `major`가 JWT에 없는 경우 (nullable)

### 9.4 Regression Risks
- 기존 가입 사용자의 로그인 플로우 깨짐
- `SsoAuthenticationFilter` 변경으로 인한 기존 인증 흐름 영향
- `MemberFixture` 시그니처 변경으로 인한 기존 테스트 컴파일 실패

---

## 10. Open Questions
- `phone` 형식 검증 수준 (정규식 강제 vs 단순 not-blank)

---

## 11. Implementation Guardrails
- `name`, `studentNumber`, `major`는 반드시 JWT에서 서버가 읽는다.
- `MemberFixture` 변경 시 기존 테스트 모두 컴파일 확인 필수
- Flyway 마이그레이션은 기존 멤버 데이터 호환성 고려 (name, phone은 NOT NULL이므로 기존 데이터 처리 전략 필요)
- `SsoAuthenticationFilter` 변경 시 `/auth/sso/**` shouldNotFilter 범위 재확인
