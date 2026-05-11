# Feat/#76 admin 접속 분기 구현

## 배경 및 필요성

기존에는 역할(Role) 정보가 SSO 토큰 또는 API Gateway 헤더에서만 전달되어, **서버 자체적으로 관리자를 지정할 수 없는 구조**였다.
이를 해결하기 위해 환경변수로 관리자 이메일 목록을 등록하고, 해당 이메일로 인증 시 자동으로 ADMIN 권한을 부여하고 회원가입까지 처리하는 기능을 추가한다.

---

## 목적

- 환경변수(`COMIT_AUTH_ADMIN_EMAILS`)로 관리자 이메일을 유연하게 관리
- 관리자 이메일 보유자가 최초 로그인 시 별도 회원가입 절차 없이 자동 등록
- 이후 요청부터 ADMIN 권한으로 모든 API 이용 가능

---

## 변경 사항

### 신규 파일

**`AdminEmailProperties.java`**
- `comit.auth.admin-emails` 프로퍼티를 `List<String>`으로 바인딩
- 환경변수: `COMIT_AUTH_ADMIN_EMAILS=admin@knu.ac.kr,super@knu.ac.kr`
- 대소문자·공백 무시하여 이메일 포함 여부 판단하는 `isAdminEmail()` 제공

---

### 변경 파일

**`ExternalIdentityMapper`**
- `toPrincipal()` 진입 시 다른 필드 검증보다 먼저 이메일을 확인
- 관리자 이메일이면 `name`, `email` 필드를 `"관리자"`로 설정하고 `role=ADMIN`으로 principal 생성

**`SsoAuthService`**
- `handleCallback()` 에서 미가입 사용자 분기 시 관리자 이메일이면 자동 회원가입 후 success 반환
- 일반 사용자는 기존과 동일하게 회원가입 페이지로 리다이렉트

**`SsoAuthenticationFilter`**
- SSO 토큰 검증 후 관리자 이메일 미가입 상태이면 자동 회원가입 처리
- `MEMBER_ALREADY_EXISTS` 예외는 무시하여 동시 요청으로 인한 중복 등록 방어

**`MemberAuthenticationFilter`**
- Bridge 환경에서도 동일하게 관리자 이메일 자동 회원가입 처리
- `MEMBER_ALREADY_EXISTS` 예외 무시로 race condition 방어

**`application.yml`**
```yaml
comit:
  auth:
    admin-emails: ${COMIT_AUTH_ADMIN_EMAILS:}
```

---

## 인증 흐름

```
로그인 요청
  └─ email이 adminEmails에 포함?
       ├─ YES → DB에 회원 없으면 자동 회원가입 (nickname="관리자-{ssoSub앞6자}")
       │         → principal: name="관리자", email="관리자", role=ADMIN
       │         → 모든 API 정상 이용 가능
       └─ NO  → 기존 흐름 유지 (미가입 시 회원가입 페이지 이동)
```

---

## 동시성 처리

관리자 이메일로 동시에 두 요청이 들어올 경우 `MemberRegistrationService`가 ssoSub 충돌을 `MEMBER_ALREADY_EXISTS`로 변환한다.
이를 각 필터·서비스에서 catch하여 무시하고 이후 DB 조회를 정상 진행한다.

```java
try {
    memberRegistrationService.register(...);
} catch (BusinessException e) {
    if (e.getErrorCode() != MemberErrorCode.MEMBER_ALREADY_EXISTS) {
        throw e;
    }
    // 동시 요청으로 이미 등록된 경우 → 무시하고 계속
}
```

---

## 테스트

- 기존 158개 테스트 전체 통과
- `SsoAuthServiceTest`: `AdminEmailProperties`, `MemberRegistrationService` mock 추가
- `AuthenticatedApiWebTest`: 동일 mock 추가
- `SsoAuthWebTest`: `AdminEmailProperties` `@Import` 추가
