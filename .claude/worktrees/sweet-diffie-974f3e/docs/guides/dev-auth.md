# Dev Auth — 로컬 / Staging 개발 로그인 가이드

> **이 기능은 `local` / `staging` 환경에서만 동작합니다.**
> SSO 없이 테스트 계정으로 세션 쿠키를 발급받을 수 있습니다.

---

## 언제 쓰나요?

- 프론트엔드 개발 중 SSO 없이 API를 연동하고 싶을 때
- Postman / curl로 인증이 필요한 API를 테스트할 때
- 관리자 권한이 필요한 Admin API를 테스트할 때

---

## 사전 준비된 테스트 계정

| nickname | role | 설명 |
|----------|------|------|
| `관리자` | ADMIN | Admin API (`/admin/**`) 접근 가능 |
| `일반유저` | STUDENT | 일반 사용자 |
| `테스트유저` | STUDENT | 일반 사용자 |

---

## 로그인

> **staging 경로 참고**: staging 환경은 리버스 프록시가 `/comit-staging/api` prefix를 제거한 뒤 서버로 전달합니다. 브라우저/curl에서 호출할 때는 `/comit-staging/api/auth/dev/login`으로 요청하세요. 로컬은 prefix 없이 `/auth/dev/login`을 사용합니다.

```bash
# 일반 사용자로 로그인 (staging)
curl -c cookies.txt -X POST \
  "https://chcse.knu.ac.kr/comit-staging/api/auth/dev/login?nickname=일반유저"

# 관리자로 로그인 (staging)
curl -c cookies.txt -X POST \
  "https://chcse.knu.ac.kr/comit-staging/api/auth/dev/login?nickname=관리자&role=ADMIN"
```

로그인 성공 시 `comit-dev-auth` 쿠키가 발급됩니다. (`200 OK`)

- `local` 기본값은 `SameSite=Lax`
- `staging` 기본값은 `SameSite=None; Secure`
- 즉 `http://localhost:5173 -> https://chcse.knu.ac.kr/comit-staging/api` 같은 cross-site 프론트 테스트도 staging에서는 바로 가능합니다.

---

## 인증이 필요한 API 호출

```bash
# 발급받은 쿠키를 함께 전송
curl -b cookies.txt \
  "https://chcse.knu.ac.kr/comit-staging/api/posts"

# Admin API 예시 (ADMIN role 로그인 후)
curl -b cookies.txt \
  "https://chcse.knu.ac.kr/comit-staging/api/admin/reports"
```

---

## 로그아웃

```bash
curl -b cookies.txt -c cookies.txt -X POST \
  "https://chcse.knu.ac.kr/comit-staging/api/auth/dev/logout"
```

---

## 로컬 개발 서버

로컬(`localhost:53080`)에서도 동일하게 사용할 수 있습니다.

```bash
curl -c cookies.txt -X POST \
  "http://localhost:53080/auth/dev/login?nickname=일반유저"
```

---

## 프론트엔드에서 사용하는 경우

```ts
// 로그인
await fetch("http://localhost:53080/auth/dev/login?nickname=일반유저", {
  method: "POST",
  credentials: "include",   // 쿠키 저장을 위해 필수
});

// 이후 API 호출 시 credentials: "include" 동일하게 유지
await fetch("http://localhost:53080/posts", {
  credentials: "include",
});
```

staging 프론트를 로컬에서 띄워 테스트할 때도 동일하게 `credentials: "include"`를 유지해야 합니다.

```ts
await fetch("https://chcse.knu.ac.kr/comit-staging/api/auth/dev/login?nickname=관리자&role=ADMIN", {
  method: "POST",
  credentials: "include",
});

await fetch("https://chcse.knu.ac.kr/comit-staging/api/admin/members?page=0&size=20", {
  credentials: "include",
});
```

---

## 주의사항

- **production 환경에서는 이 엔드포인트가 존재하지 않습니다.** (`COMIT_DEV_AUTH_ENABLED` 미설정 시 Bean 미등록)
- 테스트 계정 추가가 필요하면 `db/seed/V100__dev_seed.sql`에 INSERT IGNORE로 추가하세요.
- `role` 파라미터를 생략하면 기본값은 `STUDENT`입니다.
- dev auth 쿠키 정책은 `COMIT_DEV_AUTH_COOKIE_SAME_SITE`로 별도 제어합니다.
