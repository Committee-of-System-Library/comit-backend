# Comit Staging Verification

`comit-staging` 백엔드 staging 환경에서 실제 운영 검증을 수행한 기록.
이 문서는 "무엇을 배포했고", "어떤 문제가 있었고", "어떻게 확인했는지"를 남기는 운영 메모다.

## 목적

- `comit-staging` 백엔드가 실제로 외부 프론트와 연동 가능한지 확인한다.
- staging API 문서 경로가 live 환경에서 열리는지 확인한다.
- 첫 SSO 로그인 시 local `member` row가 생성되는지 확인한다.
- 자동 배포가 되지 않을 때 어떤 수동 절차로 우회했는지 기록한다.

## 검증 시점

- 기준 날짜: `2026-03-31`
- 대상 서버: `sidowi`
- 대상 서비스: `knu-cse-comit-server`
- 공개 경로: `https://chcse.knu.ac.kr/comit-staging/api`

## 확인한 문제

### 1. GitHub Actions deploy가 자동으로 끝나지 않음

- `main` CI는 `test`, `build-and-push`까지 성공했다.
- 하지만 `deploy` job은 계속 `queued` 상태였다.
- 원인:
  - `knu-cse-comit-server` 저장소에는 연결된 self-hosted runner가 없었다.
  - 그래서 GitHub Actions가 `sidowi`로 자동 재배포하지 못했다.

### 2. live API docs가 `500`이었음

- 초기 확인 경로:
  - `https://chcse.knu.ac.kr/comit-staging/api/docs`
- 증상:
  - `500 Internal Server Error`
- 서버 로그 원인:
  - `No static resource docs for request '/comit-staging/api/docs'`
- 해석:
  - nginx는 외부 `/comit-staging/api/docs` 요청을 내부 앱 쪽 `/docs` 계열로 넘기고 있었다.
  - 앱은 `/api/docs/**`만 열어둔 상태라 경로가 맞지 않았다.

### 3. CORS는 코드와 live 상태가 다를 수 있었음

- 검증 전에는 `https://knu-cse-comit-client.vercel.app` origin 허용 여부를 live에서 다시 확인해야 했다.
- 실제 live 상태 확인 결과:
  - 최종적으로는 `Access-Control-Allow-Origin: https://knu-cse-comit-client.vercel.app`
  - `Access-Control-Allow-Credentials: true`
  - preflight 정상 응답

## 코드 수정 내용

### 1. API docs 경로 보강

파일:
- `src/main/java/kr/ac/knu/comit/global/config/WebMvcConfig.java`
- `src/test/java/kr/ac/knu/comit/api/AuthenticatedApiWebTest.java`

수정 내용:
- `/api/docs -> /api/docs/index.html` 리다이렉트는 유지
- `/docs -> /docs/index.html` 리다이렉트 추가
- `/docs/**` 정적 리소스 핸들러 추가
- 프록시 prefix가 제거된 경로도 테스트로 고정

의도:
- nginx가 `/api` prefix를 제거해도 앱이 API 문서를 동일하게 서빙하도록 맞춘다.

### 2. CORS 허용 origin 반영

파일:
- `src/main/java/kr/ac/knu/comit/global/config/WebCorsProperties.java`
- `src/main/java/kr/ac/knu/comit/global/config/WebMvcConfig.java`
- `src/main/resources/application.yml`

수정 내용:
- `https://knu-cse-comit-client.vercel.app` 를 허용 origin에 포함
- credential 포함 preflight를 허용하는 설정 유지

## 머지된 PR

- `#18` `fix(docs): serve API docs on stripped proxy path`

이 PR은 `/docs` 경로 지원을 추가한 hotfix다.

## 실제 배포 과정

### 1. 자동 배포는 사용하지 못함

- 이유:
  - self-hosted runner 부재
  - `deploy` job이 queue에서 진행되지 않음

### 2. 수동 반영으로 우회

실제 순서:
- 로컬에서 최신 `app.jar` 빌드
- `sidowi`로 분할 업로드
- 현재 `knu-cse-comit-server` 컨테이너 안 `/app/app.jar` 교체
- 컨테이너 재시작
- 당시에는 SSO 운영값이 `comit.compose.override.yml`에 있었기 때문에 compose override를 함께 다시 적용

주의:
- 로컬에서 빌드한 Docker 이미지를 그대로 서버에 올리면 `arm64` / `amd64` 아키텍처 불일치가 날 수 있다.
- 실제로 한 번 `exec format error`가 발생했고, 즉시 이전 `amd64` 이미지로 롤백했다.
- 따라서 이 환경에서는 "이미지 통째 업로드"보다 "jar 교체 후 현재 서버 아키텍처 기반 컨테이너 유지"가 더 안전했다.

### 3. 운영값 source of truth 정리

- 이후 staging 상시 SSO 운영값은 `/opt/docker/env/comit.env`로 이동했다.
- 현재 기준:
  - `/opt/docker/env/comit.env` = staging 상시 운영값 source of truth
  - `/home/yujihun20251/comit.compose.override.yml` = 개인 Vercel 테스트, 임시 CORS 확장, 긴급 우회용
- 그래서 `knu-cse-comit-server`는 이제 base compose만으로 재기동해도 같은 SSO/CORS 설정으로 올라온다.
- `frontend-error-url`도 `https://chcse.knu.ac.kr/comit-staging/error`로 통일했다.

## live 검증 결과

### 1. API docs

검증 URL:
- `https://chcse.knu.ac.kr/comit-staging/api/docs`

최종 결과:
- `302 -> /api/docs/index.html`
- 이후 `200 OK`
- API 문서 HTML 응답 확인

### 2. CORS

검증 요청:
- `OPTIONS /members/me`
- `Origin: https://knu-cse-comit-client.vercel.app`

최종 결과:
- `200 OK`
- `Access-Control-Allow-Origin: https://knu-cse-comit-client.vercel.app`
- `Access-Control-Allow-Credentials: true`
- `Access-Control-Allow-Methods: GET,POST,PATCH,PUT,DELETE,OPTIONS`

### 3. 첫 SSO 로그인 = local member 생성

Comit은 별도 signup form이 아니라,
첫 SSO 로그인 성공 시 local `member`를 생성하는 구조다.

검증 방식:
- synthetic SSO callback JWT를 생성
- `/auth/sso/login -> /auth/sso/callback` 흐름을 통해 cookie 발급
- `/members/me` 호출
- staging DB에서 `member.sso_sub` 존재 여부 확인

검증 결과:
- 로그인 전 `member` row 수: `0`
- callback 후 `member` row 수: `1`
- `/members/me` 정상 응답 확인
- 테스트용 synthetic member는 검증 후 삭제

## 현재 결론

- `comit-staging` API docs는 live에서 접근 가능하다.
- `https://knu-cse-comit-client.vercel.app` origin에 대한 CORS는 live에서 허용된다.
- 첫 SSO 로그인 후 local member 생성 플로우도 staging에서 실제로 검증됐다.
- staging 상시 운영값은 override가 아니라 `comit.env`에서 관리된다.

## 남은 운영 이슈

### self-hosted runner 부재

현재 `knu-cse-comit-server`는:
- PR merge
- CI test
- GHCR build/push
까지는 자동화돼 있다.

하지만:
- `deploy`는 self-hosted runner가 없어서 자동 반영되지 않는다.

즉 현재 상태는:
- 코드/CI는 자동
- 서버 반영은 수동

다음 단계:
- `knu-cse-comit-server`용 self-hosted runner를 붙여서 `deploy` job이 실제로 `sidowi`까지 반영되게 해야 한다.

## 재검증 체크리스트

문제가 다시 생기면 아래 순서로 본다.

1. GitHub Actions `deploy` job이 실제로 실행됐는지 확인
2. `sidowi`에서 `docker ps`로 `knu-cse-comit-server` 재시작 시각 확인
3. `https://chcse.knu.ac.kr/comit-staging/api/docs` 가 `302 -> 200`인지 확인
4. `OPTIONS /members/me` 로 CORS 헤더 확인
5. `docker inspect knu-cse-comit-server` 로 `COMIT_AUTH_SSO_*` env가 실제 컨테이너에 들어갔는지 확인
6. `/opt/docker/env/comit.env`가 SSO/CORS 상시값 source of truth인지 확인하고, override에 중복 키가 남아 있지 않은지 본다
7. `/auth/sso/login` 이 `302`로 auth-server login URL을 반환하는지 확인
8. 필요 시 synthetic callback으로 `member` 생성 여부 확인
