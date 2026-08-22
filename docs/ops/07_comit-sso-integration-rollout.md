# Comit SSO Integration Rollout Plan

`Comit` 백엔드를 auth-server 기반 SSO와 연결하기 위한 실행 계획.

이 문서는 단순 아이디어 정리가 아니라, 실제 구현 순서와 완료 조건을 고정하는 작업 기준 문서다.

## 목적

- `Comit` 백엔드가 auth-server의 하위 서비스 custom JWT를 검증할 수 있게 만든다.
- 현재 staging의 임시 브리지 인증에서 점진적으로 벗어난다.
- `@AuthenticatedMember` 기반 현재 컨트롤러 구조를 최대한 유지한 채 인증 소스만 교체한다.
- auth-server의 관리자 권한 변경과 Keycloak 전파 문제는 별도 스트림으로 분리해 추적한다.

## 전제

- 현재 auth-server는 로그인 성공 후 하위 서비스 redirect URI에 `state`와 `token` query parameter를 붙여 redirect 한다.
- 하위 서비스용 custom JWT는 `HS256`이며 `client secret`으로 검증한다.
- 현재 `Comit`는 Spring Security resource server가 아니라 request attribute 기반 `MemberPrincipal` 주입 구조를 사용한다.
- staging에서는 기존 헤더 브리지 인증이 이미 살아 있으므로, 새 SSO 경로는 fallback을 남긴 채 도입한다.

## 목표 상태

### 1단계

- `Comit` 백엔드가 `GET /auth/sso/login` 과 `GET /auth/sso/callback` 을 제공한다.
- callback에서 custom JWT를 검증하고 HttpOnly cookie를 발급한다.
- 요청 필터가 해당 cookie를 읽어 `MemberPrincipal`을 구성하고 기존 `@AuthenticatedMember` 흐름을 유지한다.
- staging에서는 `comit.auth.sso.enabled=true` 일 때 새 SSO 경로를 사용하고, 필요 시 기존 헤더 브리지로 fallback 가능하다.

### 2단계

- 프론트는 auth-server redirect를 직접 처리하지 않고 `Comit` 백엔드 callback만 사용한다.
- 프론트는 `/members/me` 등 기존 API만 호출해 로그인 상태를 확인한다.

### 3단계

- auth-server admin의 role 변경이 `null -> ADMIN` 도 포함해 Keycloak까지 일관되게 전파된다.

## 구현 순서

실행 순서는 아래로 고정한다.

1. `Stream 3` starter / 보안 의존성 spike
2. `Stream 2` backend callback + cookie 인증
3. `Stream 1` auth-server `null -> ADMIN` 전파 문제 분리 및 수정 계획 확정

이 순서를 쓰는 이유는 다음과 같다.

- starter가 실제로 현재 코드베이스와 충돌하는지 먼저 봐야 callback 설계를 과도하게 되돌리지 않는다.
- callback과 cookie 인증은 `Comit` repo 안에서 바로 구현 가능한 경로다.
- `null -> ADMIN`은 auth-server 쪽 수정이 필요하므로 `Comit` 기능 연동과는 분리해서 처리하는 게 맞다.

## Stream 1. auth-server `null -> ADMIN` 전파 판단 및 계획

### 현재 확인된 사실

- auth-server admin에서 role 변경 시 DB `student.role`은 즉시 바뀐다.
- Keycloak 반영은 `role_change_log` 기반 비동기 sync다.
- 현재 `role_change_log.before_role` 은 `NOT NULL` 이다.
- 현재 서비스 로직은 기존 role이 있을 때만 `role_change_log`를 기록한다.

### 판단

- `Comit` 하위 서비스 custom JWT 연동만 보면 blocker는 아니다.
- 이유: custom JWT는 로그인 시점 DB의 `student.role`을 읽어 새로 발급되므로, 재로그인하면 downstream token에는 반영된다.
- 다만 auth-server admin의 기대 동작을 맞추려면 별도 수정이 맞다.

### 구현 계획

1. auth-server에서 `role_change_log.before_role` nullable 허용 또는 sentinel 값 도입
2. `AdminUserService.changeRole` 이 `null -> role` 도 로그를 남기게 수정
3. `KeycloakRoleSyncService` 가 `afterRole` 기준으로 Keycloak role 부여/변경을 수행하는지 재검증
4. 필요 시 role 제거 경로까지 함께 정의

### 완료 조건

- admin에서 `null -> ADMIN` 승격 시 `role_change_log`가 남는다.
- scheduler 실행 후 Keycloak role도 반영된다.
- 다음 access token 발급과 downstream custom JWT 모두 새 role을 반영한다.

### 소유 범위

- `Comit` repo 밖 auth-server 별도 작업

## Stream 2. Comit backend callback + 검증 + principal 주입

### 현재 확인된 사실

- 현재 인증 source는 `MemberAuthenticationFilter` 가 읽는 `X-Member-*` 헤더다.
- `MemberArgumentResolver` 는 request attribute의 `MemberPrincipal`만 읽는다.
- `MemberService.findBySso` 는 `MemberPrincipal`로 기존 로컬 회원을 조회하고 학번만 동기화한다.

### 구현 원칙

- 기존 `@AuthenticatedMember` 시그니처를 바꾸지 않는다.
- 인증 저장소는 서버 세션 대신 서명된 custom JWT cookie를 우선 사용한다.
- Spring Security 전면 전환은 이번 단계의 목표가 아니다.
- 프론트는 token을 직접 저장하지 않고 backend callback 이후 결과만 소비한다.

### 백엔드 엔드포인트

- `GET /auth/sso/login`
  - state 생성
  - state cookie 저장
  - auth-server `/login` 으로 redirect
- `GET /auth/sso/callback`
  - query의 `state`, `token` 검증
  - SSO token cookie 저장
  - 기가입자는 frontend success URL, 미가입자는 frontend register URL 로 redirect
- `GET /auth/register/prefill`
  - SSO token cookie 검증
  - JWT claim 기반 prefill(name, studentNumber, major) 반환
- `POST /auth/register`
  - SSO token cookie 검증
  - nickname, phone, agreedToTerms로 명시적 회원가입 완료
- `POST /auth/sso/logout`
  - SSO token cookie 삭제
  - state cookie 정리

### 인증 필터 동작

1. `COMIT_SSO_TOKEN` cookie 조회
2. 없으면 다음 필터로 진행
3. 있으면 HS256 + client secret으로 JWT 검증
4. `aud`, `iss`, `exp` 검증
5. claim을 `MemberPrincipal`로 변환
6. `MemberService.findBySso(...)` 호출
7. 회원이 없고 `/auth/register/**` 외 경로면 `REGISTRATION_REQUIRED`
8. 회원이 있으면 request attribute에 최종 `MemberPrincipal` 저장

### claim -> MemberPrincipal 매핑 규칙

- `sub` -> `ssoSub`
- `name` -> `name`
- `email` -> `email`
- `student_number` -> `studentNumber`
- `user_type` -> `MemberPrincipal.UserType`
- `role`
  - `ADMIN` -> `MemberPrincipal.MemberRole.ADMIN`
  - 그 외 non-null role -> `MemberPrincipal.MemberRole.STUDENT`
  - null -> `MemberPrincipal.MemberRole.STUDENT`

### 필요한 설정

- `comit.auth.sso.enabled`
- `comit.auth.sso.auth-server-base-url`
- `comit.auth.sso.client-id`
- `comit.auth.sso.client-secret`
- `comit.auth.sso.issuer`
- `comit.auth.sso.redirect-uri`
- `comit.auth.sso.frontend-success-url`
- `comit.auth.sso.frontend-register-url`
- `comit.auth.sso.frontend-error-url`
- `comit.auth.sso.cookie-path`
- `comit.auth.sso.cookie-secure`
- `comit.auth.sso.cookie-same-site`
- `comit.auth.sso.state-ttl-seconds`
- `comit.auth.sso.token-max-age-seconds`

### 위험 포인트

- nginx rewrite와 실제 redirect URI가 다르면 callback이 깨진다.
- starter와 수동 filter를 같이 올릴 때 필터 순서가 충돌할 수 있다.
- state cookie와 token cookie의 SameSite 설정이 엄격하면 redirect 이후 cookie가 누락될 수 있다.

### 완료 조건

- `/auth/sso/login` 으로 진입 시 auth-server login으로 redirect 된다.
- callback 성공 시 cookie가 생기고 `/members/me` 가 200이다.
- cookie가 없거나 만료되면 기존처럼 401이다.
- staging bridge를 끄고도 SSO cookie 경로만으로 주요 API가 동작한다.

## Stream 3. starter / 라이브러리 도입 spike

### 목적

- `knu-cse-sso-spring-boot-starter` 를 현재 코드베이스에 붙일 수 있는지 확인한다.
- 최소한 repository 설정과 optional build path를 만들고, 실제 credentials가 있을 때 바로 해석할 수 있게 한다.

### 현재 제한

- 로컬 환경에 GitHub Packages 접근용 `GITHUB_TOKEN` / `gpr.key` 가 없다.
- 따라서 이 스트림은 "설정 추가 + optional resolution path"까지 먼저 넣고, 실제 artifact fetch는 credentials 확보 후 검증한다.

### 구현 계획

1. `build.gradle` 에 GitHub Packages repository 추가
2. starter 사용 여부를 gradle property 또는 env flag로 제어
3. 활성화 시 `spring-boot-starter-security` 와 starter를 함께 넣게 구성
4. 활성화하지 않은 기본 빌드는 깨지지 않게 유지

### 완료 조건

- 기본 `./gradlew test` 는 그대로 통과한다.
- credentials와 enable flag가 있을 때 starter 의존성 해석을 시도할 수 있다.
- starter가 제공하는 타입을 붙일 위치와 충돌 지점이 문서로 정리된다.

## 세부 태스크

### T1. 실행 계획 문서와 인덱스 갱신

- 이 문서 추가
- `docs/README.md` 갱신
- `docs/ops/README.md` 갱신

### T2. Gradle과 설정 기반 준비

- GitHub Packages repository 추가
- optional starter flag 추가
- `application.yml`, `application-staging.yml`, `application-local.yml` 에 SSO 설정 뼈대 추가

### T3. auth 도메인 뼈대 추가

- `auth/controller/api`
- `auth/controller`
- `auth/service`
- `auth/dto` 또는 properties

### T4. cookie/state 기반 callback 구현

- login redirect
- callback 검증
- logout
- cookie 생성/삭제 유틸

### T5. request principal filter 구현

- cookie 읽기
- JWT 검증
- `MemberPrincipal` 변환
- 기존 `MemberArgumentResolver` 경로 재사용

### T6. 테스트

- 서비스 테스트
- 웹 테스트
- 필요 시 JWT 검증 단위 테스트

### T7. 문서와 API 산출물 갱신

- auth endpoint 계약 문서 추가
- `generateApiDocs`
- `docs/api/` 갱신

## 테스트 전략

- [테스트 전략 가이드](../guides/testing-strategy.md)를 따른다.
- 서비스 테스트에서 state 검증, JWT 검증, member 생성/재사용 규칙을 먼저 고정한다.
- 웹 테스트에서 redirect, cookie, 401/200 경계를 검증한다.
- 테스트 본문은 `// given`, `// when`, `// then` 과 각 섹션 아래 한 줄 한글 설명 주석을 유지한다.

## 현재 구현 상태

- [x] T1 실행 계획 문서와 인덱스 갱신
- [x] T2 Gradle과 설정 기반 준비
- [x] T3 auth 도메인 뼈대 추가
- [x] T4 cookie/state 기반 callback 구현
- [x] T5 request principal filter 구현
- [x] T6 테스트
- [x] T7 문서와 API 산출물 갱신

## 운영 메모 — 2단계 회원가입 이후 인증 경계

최근 SSO 2단계 회원가입 도입 이후 운영에서 특히 주의할 포인트는 아래와 같다.

1. `/auth/register/**`는 인증 필터를 건너뛴다.
- 이 경로는 callback 직후 미가입 사용자가 접근해야 하므로 `SsoAuthenticationFilter` 대상에서 제외한다.
- 대신 controller/service에서 SSO token cookie를 직접 검증한다.
- token이 없거나 비어 있으면 `401 UNAUTHORIZED`로 처리한다.

2. soft delete 회원은 재가입 화면으로 보내지 않는다.
- 동일 `ssoSub`를 가진 soft delete 회원이 있으면 callback 단계에서 register URL이 아니라 error URL로 보낸다.
- 운영 의미: “미가입”과 “탈퇴 이력 있음”을 같은 흐름으로 취급하지 않는다.
- 현재 callback reason은 `ACCOUNT_DEACTIVATED`를 사용한다.

3. `shouldNotFilter()`는 경로 세그먼트 기준으로만 예외 처리한다.
- `contains("/auth/register")` 같이 넓은 매칭은 미래 경로를 과하게 제외시킬 수 있다.
- `/auth/sso`, `/auth/sso/**`, `/auth/register`, `/auth/register/**`만 정확히 제외한다.

4. 인증 필터의 catch-all 예외는 warn 로그를 남긴다.
- 잘못된 cookie, 외부 인증 서버 장애, JWT 파싱 실패는 쿠키 제거만 하고 조용히 지나가면 운영 추적이 어렵다.
- 현재는 warn 로그 후 인증 쿠키를 제거하고 익명 요청으로 계속 진행한다.

## 남은 작업

- auth-server 쪽 `null -> ADMIN` 전파 수정
- GitHub Packages credentials 확보 후 starter 실제 resolution 검증
- staging에서 `COMIT_AUTH_SSO_ENABLED=true` 로 켜고 auth-server 앱 등록값과 redirect URI를 실제 연결
