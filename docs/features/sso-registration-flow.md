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

### Scenario A-1. soft delete 된 회원은 회원가입 페이지로 보내지지 않는다
Given:
- INTERNAL user_type을 가진 SSO 사용자가 콜백 토큰을 전달했다.
- DB에는 동일한 `ssoSub`를 가진 soft delete 회원이 존재한다.

When:
- `/auth/sso/callback`이 호출된다.

Then:
- 서버는 register URL이 아니라 error URL로 리디렉션한다.
- `reason=ACCOUNT_DEACTIVATED`로 탈퇴/비활성 계정 상태를 명시한다.

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
- soft delete 된 기존 회원을 새 회원가입 플로우로 다시 진입시키는 것

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

---

## 12. Dynamic redirectUri Extension

### 12.1 Goal
로그인 시작 시 프론트가 원하는 복귀 base URL을 넘길 수 있어야 한다.
동일 서버가 여러 프론트(chcse.knu.ac.kr, comit-sso-smoke.vercel.app 등)를 지원할 수 있다.
기존 `frontendSuccessUrl / frontendRegisterUrl / frontendErrorUrl` 설정값은
`redirectUri`가 없을 때 fallback으로 유지한다.

### 12.2 In Scope
- `GET /auth/sso/login?redirectUri=...` 선택적 파라미터
- redirectUri allowlist 검증 (origin exact match)
- redirectUri cookie 저장 및 삭제
- callback success / pendingRegistration / rejected 3분기 모두 동적 복귀 URL 반영
- stale cookie 처리

### 12.3 Out of Scope
- 여러 브라우저 탭 동시 로그인 완전 지원 (state/redirectUri 쿠키가 탭당 격리되지 않음)
- per-stage 별도 URI (`successUri`, `registerUri`, `errorUri`) 분리
- 프론트 라우팅 정책 자체 설계

### 12.4 Assumptions
- `redirectUri`는 **base URL**이다. 서버가 `?stage=success|register|error` suffix를 붙인다.
- error 경로는 `?stage=error&reason=...` 형태로 reason을 추가한다.
- 프론트는 한 URL에서 `stage` 파라미터를 읽어 분기 처리한다.
- 이 확장에서는 `stage` 기반 단일 복귀 URL 계약을 고정한다.
- 개발용 `http://localhost:*`는 http 예외를 허용한다. 그 외는 https만 허용한다.
- 한 브라우저에서 동시에 여러 SSO 로그인 시도를 완전하게 지원하는 것은 이번 범위 밖이다.

### 12.5 Frontend Usage Modes

#### Production / deployed frontend
- 운영 프론트는 `GET /auth/sso/login`만 호출해도 된다.
- 서버에 설정된 `frontendSuccessUrl`, `frontendRegisterUrl`, `frontendErrorUrl`를 기본 복귀 경로로 사용한다.
- 운영 프론트는 고정된 서비스 URL을 가지므로 `redirectUri`를 굳이 넘기지 않아도 된다.

#### Dev / smoke / preview frontend
- localhost, 개인 Vercel, smoke front, preview deploy는 `redirectUri`를 명시적으로 넘긴다.
- 이 값은 allowlist origin exact match를 통과해야 한다.
- 서버는 callback 결과에 따라 `redirectUri?stage=success|register|error` 형태로 되돌린다.
- error 케이스는 `redirectUri?stage=error&reason=...` 형식을 사용한다.

### 12.6 Routing Contract
- `redirectUri`는 `?stage=...` 단일 페이지 모델을 전제로 한다.
- `/register` 전용 경로 지원은 별도 프론트 구현 범위로 둔다.
- 운영 프론트는 fallback URL 3개(success/register/error)를 그대로 사용한다.
- dev/smoke/preview 프론트는 동일 endpoint를 호출하되 `redirectUri`로 복귀 base URL을 override한다.
- `redirectUri`에 기존 query string이 있어도 서버는 `stage`와 `reason`을 URI builder로 병합한다.

### 12.7 Scenarios

**Scenario G. allowlist에 있는 redirectUri로 로그인 — 기가입 사용자**
- `GET /auth/sso/login?redirectUri=https://comit-sso-smoke.vercel.app`
- → 콜백 후 `https://comit-sso-smoke.vercel.app?stage=success`로 302

**Scenario H. allowlist에 있는 redirectUri로 로그인 — 미가입 사용자**
- `GET /auth/sso/login?redirectUri=https://comit-sso-smoke.vercel.app`
- → 콜백 후 `https://comit-sso-smoke.vercel.app?stage=register`로 302

**Scenario I. allowlist에 없는 redirectUri**
- `GET /auth/sso/login?redirectUri=https://evil.com`
- → `400 INVALID_REQUEST`

**Scenario J. redirectUri 없이 로그인**
- `GET /auth/sso/login`
- → 기존 설정 fallback (`frontendSuccessUrl` / `frontendRegisterUrl` / `frontendErrorUrl`)
- → 이전 로그인에서 남은 redirectUri cookie가 있어도 재사용하지 않는다

**Scenario K. stale cookie가 남아 있는 상태에서 redirectUri 없이 로그인**
- 이전 로그인에서 `comit-redirect-uri` 쿠키가 브라우저에 남아 있음
- `GET /auth/sso/login` (redirectUri 파라미터 없음)
- → 서버가 `comit-redirect-uri` clear 쿠키를 Set-Cookie로 응답
- → 콜백은 설정 fallback URL 사용

### 12.8 Functional Requirements
- FR-D1: `GET /auth/sso/login`은 선택적 `redirectUri` 쿼리 파라미터를 받는다.
- FR-D2: `redirectUri`는 absolute URI여야 한다. 파싱 실패 시 `400 INVALID_REQUEST`.
- FR-D3: allowlist 검증은 **origin(scheme + host + port) exact match** 기준이다. `startsWith` 금지.
- FR-D4: `https`만 허용한다. 단 host가 `localhost`인 경우 `http` 예외 허용.
- FR-D5: 검증 실패 시 `400 INVALID_REQUEST`.
- FR-D6: 검증 통과 시 `comit-redirect-uri` 쿠키에 저장한다.
- FR-D7: `redirectUri`가 없으면 `comit-redirect-uri` clear 쿠키를 응답에 포함시킨다 (stale cookie 제거).
- FR-D8: callback success / pendingRegistration / rejected 모두 stored redirectUri를 우선 사용한다.
- FR-D9: stored redirectUri가 없으면 기존 설정 URL을 fallback으로 사용한다.
- FR-D10: callback 완료 후 `comit-redirect-uri` 쿠키는 항상 제거된다 (3분기 모두).
- FR-D11: stage suffix 조합은 문자열 연결이 아닌 **URI builder**로 한다.

### 12.9 Behavioral Rules
- `stage=success` — 기가입 INTERNAL 콜백
- `stage=register` — 미가입 INTERNAL 콜백
- `stage=error&reason=EXTERNAL_USER_NOT_ALLOWED` — EXTERNAL 콜백
- `stage=error&reason=ACCOUNT_DEACTIVATED` — soft delete 회원 콜백
- `reason`은 error 경로에만 붙는다.
- `redirectUri`에 기존 query가 있어도 서버는 기존 query를 보존하고 `stage`를 병합한다.

### 12.10 Security Constraints
- allowlist는 origin exact match. `startsWith` 또는 `contains` 검증 금지.
- `redirectUri`를 DB 저장하거나 서버 간 전달하지 않는다.
- open redirect 방지: allowlist 밖 URI는 반드시 400 응답.

### 12.11 Test Criteria
- service test: allowlist 통과 / origin mismatch 실패 / scheme mismatch 실패 / fallback / stale cookie 제거
- web test: `GET /auth/sso/login?redirectUri=...` → Set-Cookie에 `comit-redirect-uri` 포함
- callback web test: success/register/error 모두 `comit-redirect-uri` clear + 올바른 Location
- `./gradlew compileTestJava && ./gradlew test`

### 12.12 Implementation Guardrails
- `startsWith` 기반 allowlist 검증 금지
- `?stage=...` suffix는 `UriComponentsBuilder`로 조합
- `ComitSsoProperties`에 `allowedRedirectUris: List&lt;String&gt;` 및 `redirectUriCookieName` 추가
- 계약 변경 시 `SsoAuthControllerApi.java`, `docs/guides/sso-auth-flow.md`, `docs/api/` 함께 갱신
