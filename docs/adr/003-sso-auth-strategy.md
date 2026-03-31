# ADR-003: KNU CSE SSO 인증 전략

상태: `채택`

## 맥락

KNU CSE SSO (`https://chcse.knu.ac.kr/appfn/developer`)는 OAuth 2.0 유사 흐름으로 JWT를 발급한다.
comit-server가 이 SSO를 어떻게 연동하고, 자체 토큰 발급 여부를 결정해야 한다.

## SSO 연동 흐름

```text
1. 프론트 → /login?client_id=xxx&redirect_uri=xxx&state=xxx
2. 사용자 — Google Workspace(@knu.ac.kr) 계정으로 인증
3. SSO → redirect_uri?state=xxx&token=<JWT>
4. comit-server(/auth/sso/callback) — Client Secret(HMAC-SHA256)으로 JWT 서명 검증
5. comit-server — 검증된 인증 상태를 HttpOnly 쿠키로 설정
6. 이후 API 호출 — 서버가 쿠키 기반으로 사용자 인증 처리
```

## JWT Payload 클레임

| claim | 설명 | 예시 |
|---|---|---|
| `sub` | 사용자 고유 ID | `"cse-a1b2c3d4"` |
| `name` | 이름 | `"홍길동"` |
| `email` | @knu.ac.kr 이메일 | `"hong@knu.ac.kr"` |
| `studentNumber` | 학번 | `"2023012780"` |
| `userType` | 사용자 유형 | `"CSE_STUDENT"` |
| `role` | 역할 | `"STUDENT"` |
| `major` | 세부전공 (한글) | `"심화컴퓨팅 전공"` |
| `iss` | 발급자 | `"https://chcse.knu.ac.kr/appfn/api"` |
| `aud` | Client ID | — |
| `iat` / `exp` | 발급/만료 시각 (Unix) | — |

## 결정 1: Comit 자체 토큰 미발급

SSO JWT를 그대로 Bearer 토큰으로 사용한다. Comit 전용 JWT를 별도 발급하지 않는다.

**이유**
- SSO JWT 자체가 로그인 상태 증명 역할을 완전히 수행한다.
- 토큰을 두 개 관리하면 만료 동기화 문제와 갱신 로직이 이중으로 생긴다.
- SSO에 별도 refresh token 흐름이 없으므로, 만료 시 재로그인 플로우(`/login?client_id=...`)로 단순하게 처리한다.

## 결정 2: EXTERNAL 사용자 — 콜백 시점 redirect 차단

`userType == EXTERNAL`이면 SSO 콜백 단계에서 즉시 프론트 에러 페이지로 redirect한다.

```text
302 → {frontend-error-url}?reason=EXTERNAL_USER_NOT_ALLOWED
```

**초기 설계(403 반환)에서 변경한 이유**
- SSO 콜백(`/auth/sso/callback`)은 브라우저가 직접 히트하는 엔드포인트다.
  403을 반환하면 브라우저가 빈 에러 화면을 보여줄 뿐, 프론트 UI를 렌더링할 수 없다.
- redirect로 처리하면 프론트가 `reason` 파라미터를 보고 전용 안내 화면("학생만 사용 가능")을 표시할 수 있다.

**구현 세부**
- `SsoCallbackResult` sealed interface (`SsoCallbackSuccess` | `SsoCallbackRejected`) 로 콜백 결과를 타입으로 표현한다.
- 컨트롤러는 pattern matching(`switch`)으로 분기하며, `SsoCallbackRejected`는 토큰 쿠키를 세팅하지 않는다.
- 에러 URL은 `comit.auth.sso.frontend-error-url` 설정값으로 주입한다.

## 결정 3: 미등록 사용자 — 추가 회원가입 플로우

SSO 인증은 됐으나 comit DB에 없는 신규 사용자는 자동 생성하지 않고 추가 등록 절차를 거친다.

**수집 항목**
- SSO에서 pre-fill: 이름, 학번, major
- 사용자 직접 입력: 닉네임, 연락처, 개인정보 수집·이용 동의

**흐름**
```text
SSO callback 성공 + 미등록 사용자
→ 302 {frontend-register-url}
→ GET /auth/register/prefill (SSO 클레임으로 폼 pre-fill)
→ 추가 입력 완료 → POST /auth/register
→ 이후 정상 사용
```

## 현재 구현 상태 (bridge auth)

현재 `MemberAuthenticationFilter` 는 `comit.auth.bridge.enabled=true` 일 때만 `X-Member-*` 헤더를 신뢰하는 임시 브리지 방식으로 동작한다.
기본값은 비활성화이며, 현재는 `local`과 `staging` 프로필에서만 이 브리지를 켜서 사용할 계획이다.
정식 운영 전환 시에는 `Authorization: Bearer <JWT>` 직접 검증 방식으로 교체한다.

현재 헤더 ↔ 클레임 매핑:

| 헤더 | JWT claim |
|---|---|
| `X-Member-Sub` | `sub` |
| `X-Member-Name` | `name` |
| `X-Member-Email` | `email` |
| `X-Member-Student-Number` | `studentNumber` |
| `X-Member-User-Type` | `userType` |
| `X-Member-Role` | `role` |

> `major` 클레임은 현재도 `MemberPrincipal`에는 넣지 않고, 회원가입 API에서만 서버가 직접 읽어 사용한다.
