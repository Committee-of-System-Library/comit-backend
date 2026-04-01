# SSO 인증 흐름 가이드

이 문서는 Comit 서버의 SSO(Single Sign-On) 인증 구조를 **처음 보는 개발자**도 이해할 수 있도록 설명합니다.
백엔드 구조뿐 아니라 **프론트엔드에서 무엇을 해야 하는지**도 함께 다룹니다.

---

## 목차

1. [SSO가 무엇인가](#1-sso가-무엇인가)
2. [전체 흐름 한눈에 보기](#2-전체-흐름-한눈에-보기)
3. [로그인 시작 — `GET /auth/sso/login`](#3-로그인-시작--get-authssologin)
4. [콜백 처리 — `GET /auth/sso/callback`](#4-콜백-처리--get-authssocallback)
5. [API 요청 인증 처리 — 필터 체인](#5-api-요청-인증-처리--필터-체인)
6. [로그아웃 — `POST /auth/sso/logout`](#6-로그아웃--post-authssologout)
7. [JWT 토큰 구조](#7-jwt-토큰-구조)
8. [아키텍처 구조 (포트/어댑터)](#8-아키텍처-구조-포트어댑터)
9. [로컬/스테이징 개발 모드 — Bridge 인증](#9-로컬스테이징-개발-모드--bridge-인증)
10. [프론트엔드 연동 가이드](#10-프론트엔드-연동-가이드)
11. [환경 변수 목록](#11-환경-변수-목록)

---

## 1. SSO가 무엇인가

**SSO(Single Sign-On)** 는 하나의 인증 서버에서 로그인하면 여러 서비스를 별도 로그인 없이 이용할 수 있는 방식입니다.

Comit은 **KNU CSE 통합 인증 서버** (`https://chcse.knu.ac.kr/appfn/api`)를 외부 인증 공급자로 사용합니다.
사용자는 KNU 계정으로 한 번만 로그인하면 Comit 서비스를 이용할 수 있습니다.

```
사용자 ──로그인──▶ KNU 인증 서버 ──토큰 발급──▶ Comit 서버 ──쿠키 발급──▶ 사용자
```

---

## 2. 전체 흐름 한눈에 보기

```
브라우저(사용자)          Comit 서버               KNU 인증 서버
     │                       │                          │
     │ 1. GET /auth/sso/login│                          │
     │──────────────────────▶│                          │
     │                       │ state UUID 생성           │
     │ 2. 302 Redirect        │ COMIT_SSO_STATE 쿠키 발급 │
     │◀──────────────────────│                          │
     │                       │                          │
     │ 3. GET /login?client_id=...&redirect_uri=...&state=...
     │─────────────────────────────────────────────────▶│
     │                       │                          │
     │ 4. 로그인 화면 표시    │                          │
     │◀─────────────────────────────────────────────────│
     │                       │                          │
     │ (사용자가 KNU 계정으로 로그인)                    │
     │                       │                          │
     │ 5. GET /auth/sso/callback?state=...&token=...    │
     │◀─────────────────────────────────────────────────│
     │                       │                          │
     │──────────────────────▶│                          │
     │                       │ state 검증               │
     │                       │ JWT 서명/발급자/만료 검증  │
     │ 6. 302 Redirect        │ COMIT_SSO_TOKEN 쿠키 발급 │
     │◀──────────────────────│ COMIT_SSO_STATE 쿠키 삭제 │
     │                       │                          │
     │ 7. 프론트엔드 복귀 URL로 이동 (stage=...)        │
     │                       │                          │
     │ 8. GET /api/posts (이후 모든 API 요청)            │
     │ Cookie: COMIT_SSO_TOKEN=xxx                      │
     │──────────────────────▶│                          │
     │                       │ 쿠키에서 토큰 추출        │
     │                       │ JWT 검증                  │
     │                       │ MemberPrincipal 생성      │
     │ 9. 200 OK + 응답 데이터│                          │
     │◀──────────────────────│                          │
```

---

## 3. 로그인 시작 — `GET /auth/sso/login`

### 프론트엔드가 해야 할 일

단순히 이 URL로 이동시키면 됩니다. 필요하면 `redirectUri`를 함께 넘겨 동적 복귀 URL을 지정할 수 있습니다.
`redirectUri`는 allowlist origin exact match 검증을 통과해야 하며, 콜백 결과는 `stage=success|register|error`로 돌아옵니다.

```javascript
// 로그인 버튼 클릭 시
window.location.href = "https://api.comit.kr/auth/sso/login";
```

백엔드가 알아서 KNU 인증 서버로 리다이렉트해 줍니다.

### 백엔드에서 일어나는 일

**관련 파일:** `SsoAuthController.java`, `SsoAuthService.java`

1. `UUID.randomUUID()`로 `state` 값을 생성합니다.
   - state는 **CSRF 공격 방지**용입니다. 로그인 시작 시 생성한 값과 콜백에서 돌아온 값이 일치해야만 진행됩니다.

2. `COMIT_SSO_STATE` 쿠키를 발급합니다. (HttpOnly, 5분 유효)

3. `redirectUri`가 있으면 allowlist 검증 후 `comit-redirect-uri` 쿠키를 저장하고, 없으면 이전 쿠키를 제거합니다.

4. KNU 인증 서버 로그인 URL로 302 리다이렉트합니다.
   ```
   https://chcse.knu.ac.kr/appfn/api/login
     ?client_id={clientId}
     &redirect_uri={comit서버의 콜백 URL}
     &state={uuid}
   ```

```
GET /auth/sso/login
  └─▶ SsoAuthController.startLogin()
       └─▶ SsoAuthService.startLogin()
            ├─ state = UUID 생성
            ├─ ExternalAuthClient.buildLoginRedirectUrl(state)  ← 로그인 URL 조합
            └─ AuthCookieManager.createStateCookie(state)       ← state 쿠키 생성
  └─▶ 302 응답
       ├─ Set-Cookie: COMIT_SSO_STATE=...
       └─ Location: https://chcse.knu.ac.kr/appfn/api/login?...
```

---

## 4. 콜백 처리 — `GET /auth/sso/callback`

### 프론트엔드가 해야 할 일

**아무것도 하지 않아도 됩니다.**

KNU 인증 서버가 직접 이 URL을 호출합니다. 백엔드가 처리를 완료하면 자동으로 `redirectUri?stage=success|register|error` 또는 기존 `frontendSuccessUrl` / `frontendRegisterUrl` / `frontendErrorUrl`로 리다이렉트되므로,
프론트엔드는 그 URL에서 로그인 완료 상태를 시작하면 됩니다.

### 백엔드에서 일어나는 일

**관련 파일:** `SsoAuthController.java`, `SsoAuthService.java`, `CustomJwtExternalAuthClient.java`

KNU 인증 서버가 다음 형태로 콜백을 호출합니다:
```
GET /auth/sso/callback?state={uuid}&token={jwt}
```

처리 순서:

1. **state 검증** — 요청 파라미터의 `state`와 쿠키(`COMIT_SSO_STATE`)의 값이 일치하는지 확인합니다.
   불일치 시 `INVALID_REQUEST` 에러를 반환합니다.

2. **JWT 검증** — `CustomJwtExternalAuthClient.verify(token)`에서 다음을 순서대로 확인합니다.
   - 알고리즘이 HS256인지
   - `clientSecret`으로 서명이 유효한지
   - `issuer`가 설정값과 일치하는지
   - `audience`에 `clientId`가 포함되는지
   - 만료(`exp`)가 지나지 않았는지

   하나라도 실패하면 `UNAUTHORIZED` 에러를 반환합니다.

3. **EXTERNAL 사용자 거부** — JWT 검증 통과 후, `userType == EXTERNAL`인 사용자는 가입을 거부합니다.
   이 경우 쿠키를 발급하지 않고 `stage=error&reason=EXTERNAL_USER_NOT_ALLOWED`로 302 리다이렉트합니다.
   (`SsoCallbackRejected` 결과 반환)

4. **가입 상태 확인** — 정상 사용자(`CSE_STUDENT` / `KNU_OTHER_DEPT`) 검증 성공 후 `member.sso_sub`로 가입 여부를 확인합니다.
   - 기존 회원이면 `SsoCallbackSuccess`
   - 미가입 회원이면 `SsoCallbackPendingRegistration`

5. **쿠키 처리** — 성공/미가입/거부 분기 모두:
   - `COMIT_SSO_TOKEN` 쿠키 발급 (HttpOnly, 1시간 유효)
   - `COMIT_SSO_STATE` 쿠키 삭제 (maxAge=0)
   - `comit-redirect-uri` 쿠키 삭제 (동적 복귀 URL 사용 여부와 무관하게 콜백 후 항상 제거)

6. **302 리다이렉트**
   - 기존 회원 → `redirectUri?stage=success` 또는 `frontendSuccessUrl`
   - 미가입 회원 → `redirectUri?stage=register` 또는 `frontendRegisterUrl`
   - 외부 사용자 → `redirectUri?stage=error&reason=EXTERNAL_USER_NOT_ALLOWED` 또는 `frontendErrorUrl`

```
GET /auth/sso/callback?state=X&token=Y
  └─▶ SsoAuthController.handleCallback()
       ├─ AuthCookieManager.resolveStateCookie(request)        ← 쿠키에서 state 읽기
       └─▶ SsoAuthService.handleCallback(state, token, storedState, storedRedirectUri) → SsoCallbackResult
            ├─ validateState(state, storedState)               ← CSRF 방어
            ├─ ExternalAuthClient.verify(token)                ← JWT 검증
            │    └─▶ CustomJwtExternalAuthClient
            │         ├─ HS256 알고리즘 확인
            │         ├─ 서명 검증 (clientSecret)
            │         ├─ issuer 검증
            │         ├─ audience 검증 (clientId)
            │         └─ 만료 시간 검증
            └─ userType == EXTERNAL?
                 ├─ YES → SsoCallbackRejected(errorUrl)
                 └─ NO  → MemberService.hasActiveMember(ssoSub)
                           ├─ true  → SsoCallbackSuccess(successUrl, cookies)
                           └─ false → SsoCallbackPendingRegistration(registerUrl, cookies)
  └─▶ SsoCallbackSuccess              → 302 Location: {redirectUri?stage=success or frontendSuccessUrl}
  └─▶ SsoCallbackPendingRegistration  → 302 Location: {redirectUri?stage=register or frontendRegisterUrl}
  └─▶ SsoCallbackRejected             → 302 Location: {redirectUri?stage=error&reason=... or frontendErrorUrl}
```

---

## 5. API 요청 인증 처리 — 필터 체인

로그인 이후 모든 API 요청에서 인증이 처리되는 방식입니다.

**관련 파일:** `SsoAuthenticationFilter.java`, `ExternalIdentityMapper.java`, `MemberArgumentResolver.java`

### 필터 실행 조건

- `comit.auth.sso.enabled=true` 일 때만 활성화됩니다.
- `/auth/sso/**`, `/auth/register/**` 경로는 필터를 건너뜁니다. (`shouldNotFilter`)
- 모든 요청에 대해 가장 먼저 실행됩니다. (`@Order(HIGHEST_PRECEDENCE)`)

### 처리 순서

```
HTTP 요청 (Cookie: COMIT_SSO_TOKEN=xxx)
  └─▶ SsoAuthenticationFilter.doFilterInternal()
       │
       ├─ 이미 인증된 요청이면 스킵
       │
       ├─ AuthCookieManager.resolveTokenCookie(request)
       │   └─ 쿠키 없으면 → 비인증 상태로 다음 필터 진행
       │
       ├─ ExternalAuthClient.verify(token)
       │   └─ ExternalIdentity { ssoSub, name, email, studentNumber, userType, role }
       │
       ├─ ExternalIdentityMapper.toPrincipal(identity)
       │   └─ MemberPrincipal (memberId=null, 임시)
       │
       ├─ MemberService.findBySso(provisionalPrincipal)
       │   └─ DB에서 ssoSub으로 조회 → 없으면 pending registration
       │
       ├─ member가 없고 요청 경로가 /auth/register/** 외부면
       │   └─ REGISTRATION_REQUIRED
       │
       ├─ member가 있으면 ExternalIdentityMapper.toPrincipal(member.getId(), identity)
       │   └─ MemberPrincipal (memberId=실제 DB ID, 확정)
       │
       └─ request.setAttribute("memberPrincipal", authenticatedPrincipal)

컨트롤러에서 사용:
  public ResponseEntity<?> someApi(
      @AuthenticatedMember MemberPrincipal principal  ← 주입됨
  ) { ... }
```

### MemberPrincipal 구조

```java
record MemberPrincipal(
    Long memberId,       // Comit DB의 회원 ID
    String ssoSub,       // KNU 인증 서버의 고유 사용자 식별자
    String name,         // 이름
    String email,        // 이메일
    String studentNumber,// 학번 (null 가능)
    UserType userType,   // CSE_STUDENT | KNU_OTHER_DEPT | EXTERNAL
    MemberRole role      // ADMIN | STUDENT
)
```

---

## 6. 로그아웃 — `POST /auth/sso/logout`

### 프론트엔드가 해야 할 일

```javascript
await fetch("/auth/sso/logout", { method: "POST", credentials: "include" });
// 이후 로그인 페이지로 이동
```

### 백엔드에서 일어나는 일

`COMIT_SSO_TOKEN` 쿠키를 `maxAge=0`으로 설정해 삭제합니다.
KNU 인증 서버와는 별도로 통신하지 않습니다. Comit 서버의 세션 쿠키만 제거합니다.

---

## 7. JWT 토큰 구조

KNU 인증 서버가 발급하는 커스텀 JWT의 페이로드 구조입니다.

**관련 파일:** `CustomJwtExternalAuthClient.java`, `ExternalIdentity.java`

| 클레임 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `sub` | String | ✅ | KNU 인증 서버의 사용자 고유 식별자 |
| `name` | String | ✅ | 사용자 이름 |
| `email` | String | ✅ | 이메일 주소 |
| `student_number` | String | ❌ | 학번 (없을 수 있음) |
| `user_type` | String | ✅ | `CSE_STUDENT` / `KNU_OTHER_DEPT` / `EXTERNAL` |
| `role` | String | ❌ | `ADMIN` / `STUDENT` (없으면 STUDENT 기본값) |
| `iss` | String | ✅ | 토큰 발급자 (설정의 `issuer`와 일치해야 함) |
| `aud` | List | ✅ | 토큰 수신자 (`clientId`가 포함되어야 함) |
| `exp` | Long | ✅ | 만료 시각 (Unix timestamp) |

**서명 알고리즘:** HS256 (HMAC-SHA256)
**서명 키:** `clientSecret` (환경 변수 `COMIT_AUTH_SSO_CLIENT_SECRET`)

---

## 8. 아키텍처 구조 (포트/어댑터)

이 구조는 **포트/어댑터 패턴**을 적용하여 외부 인증 공급자에 대한 의존을 격리합니다.

```
┌─────────────────────────────────────────────────────────┐
│                      Service Layer                       │
│                                                         │
│  SsoAuthService          AuthCookieManager              │
│  (로그인/콜백 오케스트레이션)  (쿠키 생성/삭제/조회)         │
│                                                         │
│  ExternalIdentityMapper                                 │
│  (ExternalIdentity → MemberPrincipal 변환)              │
└──────────────────────┬──────────────────────────────────┘
                       │ uses
              ┌────────▼────────┐
              │   Port (인터페이스)│
              │                 │
              │ ExternalAuthClient        ExternalIdentity
              │ - buildLoginRedirectUrl() (외부 ID 데이터)
              │ - verify()               │
              └────────┬────────┘
                       │ implements
              ┌────────▼────────────────────┐
              │   Adapter (구현체)            │
              │                             │
              │  CustomJwtExternalAuthClient │
              │  (KNU 커스텀 JWT 검증 구현)   │
              └─────────────────────────────┘
```

**왜 이 구조인가?**

`SsoAuthService`와 `SsoAuthenticationFilter`는 `ExternalAuthClient` **인터페이스**만 바라봅니다.
나중에 KNU 인증 방식이 바뀌거나 다른 인증 공급자를 추가하더라도, 새 구현체만 만들면 됩니다.
서비스 로직은 건드릴 필요가 없습니다.

---

## 9. 로컬/스테이징 개발 모드 — Bridge 인증

실제 KNU 인증 서버 없이 로컬에서 개발할 수 있도록 **Bridge 인증 모드**를 제공합니다.

**관련 파일:** `MemberAuthenticationFilter.java`

### 활성화 조건

`application-local.yml`에서 자동으로 활성화됩니다:

```yaml
comit:
  auth:
    bridge:
      enabled: true  # 헤더 기반 인증 활성화
    sso:
      enabled: false # SSO 쿠키 기반 인증 비활성화
```

### 사용 방법

API 요청 시 다음 HTTP 헤더를 추가합니다:

```http
X-Member-Sub: test-user-001
X-Member-Name: 홍길동
X-Member-Email: hong@knu.ac.kr
X-Member-Student-Number: 2021012345
X-Member-User-Type: CSE_STUDENT
X-Member-Role: STUDENT
```

| 헤더 | 필수 | 설명 |
|------|------|------|
| `X-Member-Sub` | ✅ | 없으면 비인증 처리 |
| `X-Member-Name` | ❌ | 없으면 `comit-user` 기본값 |
| `X-Member-Email` | ❌ | |
| `X-Member-Student-Number` | ❌ | |
| `X-Member-User-Type` | ❌ | 없으면 `CSE_STUDENT` 기본값 |
| `X-Member-Role` | ❌ | 없으면 `STUDENT` 기본값 |

> **주의:** Bridge 모드는 프로덕션에서는 절대 활성화하면 안 됩니다. 헤더 위조로 누구든 인증을 우회할 수 있습니다.

---

## 10. 프론트엔드 연동 가이드

### 로그인

```javascript
// 로그인 버튼 클릭 핸들러
function handleLogin() {
  // 현재 페이지 저장이 필요하다면 localStorage 등에 미리 저장
  window.location.href = "/auth/sso/login";
  // 이 이후는 브라우저가 자동으로 처리 (리다이렉트 연쇄)
}
```

### 로그인 성공 감지

백엔드는 로그인 성공/회원가입 대기/외부 거부 결과에 따라 `redirectUri?stage=success|register|error` 또는 기존 `frontendSuccessUrl` / `frontendRegisterUrl` / `frontendErrorUrl`로 리다이렉트합니다.
이 URL을 진입 경로로 사용하면 됩니다.

```javascript
// 예: redirectUri = "http://localhost:5173/auth/return"
// 해당 페이지 컴포넌트에서 stage를 보고 분기 처리
function AuthSuccessPage() {
  useEffect(() => {
    const stage = new URLSearchParams(window.location.search).get("stage");
    if (stage === "success") {
      navigate("/home");
    } else if (stage === "register") {
      navigate("/auth/register");
    } else if (stage === "error") {
      setErrorMessage(new URLSearchParams(window.location.search).get("reason"));
    }
  }, []);
}
```

### API 요청

**쿠키가 자동으로 전송**되므로 별도 토큰 관리가 필요 없습니다.
단, CORS 환경에서는 `credentials: "include"` 가 반드시 필요합니다.

```javascript
// fetch 사용 시
const response = await fetch("/api/posts", {
  credentials: "include",  // 쿠키 자동 전송 필수
});

// axios 사용 시
axios.defaults.withCredentials = true;
// 또는
const response = await axios.get("/api/posts", {
  withCredentials: true,
});
```

### 인증 실패 처리

```javascript
// 401 Unauthorized 응답 처리
if (response.status === 401) {
  // 쿠키가 만료되었거나 없는 상태 → 로그인 페이지로
  window.location.href = "/auth/sso/login";
}
```

### 로그아웃

```javascript
async function handleLogout() {
  await fetch("/auth/sso/logout", {
    method: "POST",
    credentials: "include",
  });
  window.location.href = "/login";
}
```

### 쿠키 동작 요약

| 쿠키 이름 | 역할 | 유효 시간 | 특징 |
|-----------|------|-----------|------|
| `COMIT_SSO_STATE` | CSRF 방지용 state 저장 | 5분 | 로그인 완료 후 자동 삭제 |
| `comit-redirect-uri` | 로그인 후 복귀 base URL 저장 | state TTL과 동일 | allowlist 검증 통과 시에만 저장, 콜백 완료 후 자동 삭제 |
| `COMIT_SSO_TOKEN` | API 인증 토큰 | 1시간 | HttpOnly — JS에서 접근 불가 |

> `HttpOnly` 쿠키는 JavaScript에서 읽을 수 없습니다.
> 이는 의도된 보안 설계이며, 로그인 여부는 API 응답(200 vs 401)으로 판단해야 합니다.

---

## 11. 환경 변수 목록

| 환경 변수 | 기본값 | 설명 |
|-----------|--------|------|
| `COMIT_AUTH_SSO_ENABLED` | `false` | SSO 인증 필터 활성화 여부 |
| `COMIT_AUTH_SSO_AUTH_SERVER_BASE_URL` | `https://chcse.knu.ac.kr/appfn/api` | KNU 인증 서버 기본 URL |
| `COMIT_AUTH_SSO_CLIENT_ID` | (필수) | Comit 서비스의 클라이언트 ID |
| `COMIT_AUTH_SSO_CLIENT_SECRET` | (필수) | JWT 서명 검증용 비밀 키 |
| `COMIT_AUTH_SSO_ISSUER` | `https://chcse.knu.ac.kr/appfn/api` | JWT `iss` 클레임 검증값 |
| `COMIT_AUTH_SSO_REDIRECT_URI` | (필수) | 콜백 URL (`/auth/sso/callback` 전체 경로) |
| `COMIT_AUTH_SSO_FRONTEND_SUCCESS_URL` | `http://localhost:5173` | 로그인 성공 후 리다이렉트 URL |
| `COMIT_AUTH_SSO_FRONTEND_REGISTER_URL` | `http://localhost:5173/register` | 미가입 사용자 리다이렉트 URL |
| `COMIT_AUTH_SSO_FRONTEND_ERROR_URL` | `http://localhost:5173/error` | 가입 거부(EXTERNAL 사용자) 시 리다이렉트 URL |
| `COMIT_AUTH_SSO_ALLOWED_REDIRECT_URIS` | `https://chcse.knu.ac.kr,...` | 동적 redirectUri 확장에서 허용할 origin allowlist |
| `COMIT_AUTH_SSO_REDIRECT_URI_COOKIE_NAME` | `comit-redirect-uri` | 동적 복귀 base URL을 잠시 보관하는 쿠키 이름 |
| `COMIT_AUTH_SSO_TOKEN_COOKIE_NAME` | `COMIT_SSO_TOKEN` | 인증 토큰 쿠키 이름 |
| `COMIT_AUTH_SSO_STATE_COOKIE_NAME` | `COMIT_SSO_STATE` | state 쿠키 이름 |
| `COMIT_AUTH_SSO_STATE_TTL_SECONDS` | `300` | state 쿠키 유효 시간 (초) |
| `COMIT_AUTH_SSO_TOKEN_MAX_AGE_SECONDS` | `3600` | 토큰 쿠키 유효 시간 (초) |
| `COMIT_AUTH_SSO_COOKIE_PATH` | `/` | 쿠키 적용 경로 |
| `COMIT_AUTH_SSO_COOKIE_SECURE` | `true` | HTTPS 전용 쿠키 여부 |
| `COMIT_AUTH_SSO_COOKIE_SAME_SITE` | `Lax` | SameSite 정책 |
| `COMIT_AUTH_BRIDGE_ENABLED` | `false` | 헤더 기반 Bridge 인증 활성화 여부 |

> 동적 redirectUri 확장에서는 `allowedRedirectUris`와 `redirectUriCookieName` 설정이 추가됩니다. 자세한 계약은 [`docs/features/sso-registration-flow.md`](../features/sso-registration-flow.md) Section 12를 따릅니다.

---

## 관련 소스 파일 경로

```
src/main/java/kr/ac/knu/comit/
├── auth/
│   ├── config/
│   │   └── ComitSsoProperties.java          # 설정 프로퍼티 바인딩
│   ├── controller/
│   │   ├── SsoAuthController.java           # 엔드포인트 구현
│   │   └── api/SsoAuthControllerApi.java    # API 계약 (Swagger 포함)
│   ├── dto/
│   │   ├── SsoLoginStart.java               # 로그인 시작 응답 DTO
│   │   └── SsoCallbackSuccess.java          # 콜백 성공 응답 DTO
│   ├── port/
│   │   ├── ExternalAuthClient.java          # 외부 인증 클라이언트 인터페이스
│   │   └── ExternalIdentity.java            # 외부 신원 정보 모델
│   ├── infrastructure/customjwt/
│   │   └── CustomJwtExternalAuthClient.java # KNU JWT 검증 구현체
│   └── service/
│       ├── SsoAuthService.java              # 로그인/콜백 오케스트레이션
│       ├── AuthCookieManager.java           # 쿠키 생성/삭제/조회
│       └── ExternalIdentityMapper.java      # ExternalIdentity → MemberPrincipal
└── global/auth/
    ├── SsoAuthenticationFilter.java         # SSO 쿠키 기반 인증 필터
    ├── MemberAuthenticationFilter.java      # Bridge 헤더 기반 인증 필터
    ├── MemberPrincipal.java                 # 인증 컨텍스트 레코드
    ├── MemberArgumentResolver.java          # @AuthenticatedMember 처리
    └── AuthenticatedMember.java             # 컨트롤러 파라미터 애노테이션
```
