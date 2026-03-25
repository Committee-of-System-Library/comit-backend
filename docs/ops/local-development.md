# 로컬 실행 가이드

현재 로컬 개발은 `local` 프로필 + Docker MySQL + 임시 header 기반 인증 브리지를 기준으로 맞춰져 있다.

## 포트와 계정

| 대상 | 값 |
|---|---|
| 앱 포트 | `53080` |
| MySQL 포트 | `53306` |
| DB 이름 | `comit` |
| DB 계정 | `root` |
| DB 비밀번호 | `root` |

관련 파일

- [application-local.yml](../../src/main/resources/application-local.yml)
- [compose.local.yml](../../compose.local.yml)
- [.env.local.example](../../.env.local.example)

## 실행 순서

1. 로컬 MySQL을 띄운다.

```bash
docker compose -f compose.local.yml up -d
```

2. 애플리케이션을 `local` 프로필로 실행한다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

3. 종료가 필요하면 Docker 컨테이너를 내린다.

```bash
docker compose -f compose.local.yml down
```

## 로그 위치

- 애플리케이션 로그: `./logs/app-local.log`

## 인증 방식

실제 KNU CSE SSO starter 연동 전까지는 [MemberAuthenticationFilter.java](../../src/main/java/kr/ac/knu/comit/global/auth/MemberAuthenticationFilter.java)가 `local` 프로필에서만 요청 헤더를 읽어 임시 인증 컨텍스트를 만든다.

현재 사용하는 헤더

- `X-Member-Sub`
- `X-Member-Name`
- `X-Member-Email`
- `X-Member-Student-Number`
- `X-Member-User-Type`
- `X-Member-Role`

핵심 규칙

- `X-Member-Sub`가 없으면 인증 사용자가 없는 요청으로 처리한다.
- `X-Member-Sub`가 있으면 회원을 조회하거나 최초 로그인으로 생성한다.
- Controller에서는 `@AuthenticatedMember MemberPrincipal`로 현재 사용자를 받는다.

## 요청 예시

```bash
curl -X GET 'http://localhost:53080/posts?boardType=QNA&size=20' \
  -H 'X-Member-Sub: 20260001' \
  -H 'X-Member-Name: Comit User' \
  -H 'X-Member-Email: comit@example.com' \
  -H 'X-Member-Student-Number: 20260001' \
  -H 'X-Member-User-Type: CSE_STUDENT' \
  -H 'X-Member-Role: STUDENT'
```

```bash
curl -X PATCH 'http://localhost:53080/members/me' \
  -H 'Content-Type: application/json' \
  -H 'X-Member-Sub: 20260001' \
  -H 'X-Member-Name: Comit User' \
  -d '{"nickname":"new-nickname"}'
```

## 주의사항

- 이 인증 방식은 로컬 개발용 임시 브리지다.
- 실제 인증 구조로 바뀌면 header 기반 설명은 제거하고 SSO starter 기준 문서로 교체해야 한다.
- 로컬 DB 스키마는 Flyway migration으로 자동 반영된다.
