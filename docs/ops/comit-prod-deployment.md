# COMIT Server Prod Deployment Plan

**Date:** 2026-04-10
**Target:** 다음주 prod 배포 (sidowi 서버, 155.230.29.100:2803)
**Current State:** staging 환경 운영 중 (`/comit-staging` 경로)

---

## P0 -- 배포 불가 / 데이터 손실 / 보안 위험

### P0-1. `application-prod.yml` 생성

**Why:** prod 프로파일 설정이 없으면 서버가 기동 자체가 안 되거나, staging 설정(dev seed 데이터 포함)으로 뜰 위험이 있다.

**작업 내용:**
- `src/main/resources/application-prod.yml` 생성
- `application-staging.yml` 기반으로 작성하되 아래 항목 반드시 변경:
  - `spring.flyway.locations`: **`classpath:db/migration`만 포함** (`classpath:db/seed` 제외 필수)
  - `comit.dev.auth.enabled`: **`false`** (DevAuthController 비활성화 -- prod에 dev 로그인 경로 노출 방지)
  - `comit.auth.bridge.enabled`: **`false`**
  - `comit.auth.sso.enabled`: **`true`** (SSO만 사용)
  - `comit.auth.sso.frontend-success-url`: `https://chcse.knu.ac.kr/comit`
  - `comit.auth.sso.frontend-error-url`: `https://chcse.knu.ac.kr/comit/error`
  - `comit.auth.sso.frontend-register-url`: `https://chcse.knu.ac.kr/comit/register`
  - `comit.auth.sso.cookie-path`: `/comit` (또는 `/`)
  - `logging.file.name`: `/app/logs/app-prod.log`
  - `spring.threads.virtual.enabled`: `true`

**Acceptance Criteria:**
- [ ] `application-prod.yml`이 존재하고 `classpath:db/seed`가 flyway locations에 없음
- [ ] `comit.dev.auth.enabled`가 `false`
- [ ] SSO 관련 URL이 모두 `/comit` 경로를 가리킴

---

### P0-2. prod `comit.env` 환경변수 파일 준비

**Why:** 컨테이너에 주입할 환경변수가 없으면 DB 접속, SSO 인증 모두 실패한다.

**작업 내용:**
- 서버에 `/opt/docker/env/comit-prod.env` (또는 동등 위치) 생성
- 필수 변수:
  ```
  SPRING_PROFILES_ACTIVE=prod
  DB_URL=jdbc:mysql://cse-db:3306/comit?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
  DB_USERNAME=<prod_user>
  DB_PASSWORD=<prod_password>
  COMIT_AUTH_SSO_ENABLED=true
  COMIT_AUTH_BRIDGE_ENABLED=false
  COMIT_DEV_AUTH_ENABLED=false
  COMIT_AUTH_SSO_CLIENT_ID=<prod_client_id>
  COMIT_AUTH_SSO_CLIENT_SECRET=<prod_client_secret>
  COMIT_AUTH_SSO_REDIRECT_URI=https://chcse.knu.ac.kr/comit/api/auth/sso/callback
  COMIT_AUTH_SSO_FRONTEND_SUCCESS_URL=https://chcse.knu.ac.kr/comit
  COMIT_AUTH_SSO_FRONTEND_ERROR_URL=https://chcse.knu.ac.kr/comit/error
  S3_BUCKET_NAME=<prod_bucket>
  S3_ACCESS_KEY=<prod_key>
  S3_SECRET_KEY=<prod_secret>
  S3_BASE_URL=<prod_s3_url>
  LOG_FILE_PATH=/app/logs/app-prod.log
  ```
- Pinpoint agent 변수: `agentId=comit-prod-01`, `applicationName=comit-backend`

**Acceptance Criteria:**
- [ ] env 파일이 서버에 존재하고, staging과 값이 분리됨
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] DB URL이 `comit` 스키마를 가리킴 (comit_staging 아님)

---

### P0-3. prod DB 스키마 및 계정 준비

**Why:** DB가 없으면 Flyway 마이그레이션이 실패하고 서버가 기동 불가.

**작업 내용:**
- `cse-db` MySQL에 `comit` 데이터베이스 생성 (이미 있으면 확인)
- prod 전용 DB 유저 생성 및 권한 부여 (`SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP` on `comit.*`)
- Flyway가 V1~V12 마이그레이션을 정상 수행하는지 로컬에서 빈 DB로 검증

**Acceptance Criteria:**
- [ ] `comit` 데이터베이스가 `cse-db`에 존재
- [ ] prod 전용 유저로 접속 가능
- [ ] Flyway V1~V12 마이그레이션이 빈 DB에서 정상 완료

---

### P0-4. Keycloak (auth-server)에 prod redirect URI 등록

**Why:** SSO callback URI가 등록되지 않으면 로그인 자체가 불가능.

**작업 내용:**
- auth-server 팀에 요청: `https://chcse.knu.ac.kr/comit/api/auth/sso/callback`를 redirect URI로 등록
- `allowed-redirect-uris` 목록에 `https://chcse.knu.ac.kr/comit` 추가 필요 여부 확인
- prod용 `client-id`, `client-secret` 발급 (staging과 분리할지 결정)

**Acceptance Criteria:**
- [ ] auth-server에 prod callback URI가 등록됨
- [ ] prod용 client credentials 확보

---

### P0-5. nginx `comit.conf`에 `/comit` 경로 추가

**Why:** nginx 라우팅이 없으면 외부에서 prod 서버에 접근 불가.

**작업 내용:**
- `knu-cse-proxy` nginx 설정에 `/comit` location 블록 추가
- upstream을 prod 컨테이너명으로 지정 (예: `comit-server-blue`)
- staging과 분리: `/comit-staging`은 기존 유지, `/comit`은 prod로 라우팅
- `proxy_set_header`, `X-Forwarded-*` 헤더 설정 (staging 설정 참고)

**Acceptance Criteria:**
- [ ] `https://chcse.knu.ac.kr/comit/api/...`로 요청이 prod 컨테이너에 도달
- [ ] `/comit-staging` 경로는 기존대로 staging 컨테이너에 도달
- [ ] nginx reload 후 502 없음

---

## P1 -- prod 품질 미달이지만 배포는 가능

### P1-1. PR #59 (healthcheck) 머지 및 첫 자동 배포 검증

**Why:** actuator healthcheck가 없으면 컨테이너 이상 시 자동 감지가 안 된다. CI/CD 파이프라인 전체 흐름도 검증 필요.

**작업 내용:**
- PR #59 리뷰 완료 후 main 머지
- GitHub Actions CI/CD 워크플로우(`.github/workflows/deploy.yml`)가 정상 동작하는지 확인
  - test -> build-and-push (GHCR) -> deploy (self-hosted runner)
- `docker-compose.services.yml`에 prod 서비스 정의 추가 (env_file, container_name 분리)
- healthcheck endpoint (`/actuator/health`) 응답 확인

**Acceptance Criteria:**
- [ ] main 머지 후 GHCR에 이미지 push 성공
- [ ] self-hosted runner에서 컨테이너 정상 기동
- [ ] `/actuator/health` 200 OK 응답

---

### P1-2. deploy.yml 워크플로우 prod/staging 분기 처리

**Why:** 현재 워크플로우는 staging 단일 환경만 지원. prod 배포 시 staging 컨테이너가 교체될 위험.

**작업 내용:**
- `deploy.yml`의 deploy job에서 prod/staging 컨테이너를 분리하는 로직 추가
- 방법 선택:
  - (A) `docker-compose.services.yml`에 prod 서비스를 별도 정의하고, workflow에서 분기
  - (B) 별도 `deploy-prod.yml` 워크플로우 생성 (release tag 기반 트리거)
- prod 배포 트리거: `workflow_dispatch` 또는 release tag (`v*`)

**Acceptance Criteria:**
- [ ] main push 시 staging만 배포됨 (prod 영향 없음)
- [ ] prod 배포는 명시적 트리거로만 실행

---

### P1-3. Pinpoint agent prod 설정

**Why:** 모니터링이 없으면 prod 장애 시 원인 분석이 어렵다.

**작업 내용:**
- prod 컨테이너에 Pinpoint agent 환경변수 설정: `agentId=comit-prod-01`
- Pinpoint 서버에서 `comit-prod-01` 에이전트가 정상 연결되는지 확인
- staging(`comit-stg-01`)과 분리되어 대시보드에서 구분 가능한지 확인

**Acceptance Criteria:**
- [ ] Pinpoint 대시보드에 `comit-prod-01` 에이전트 표시
- [ ] staging과 prod 트래픽이 분리되어 보임

---

## P2 -- 있으면 좋고 나중에 해도 무방

### P2-1. Blue-Green 배포 스크립트

**Why:** 무중단 배포가 되면 좋지만, 초기 prod 트래픽이 적다면 단순 재시작으로도 충분.

**작업 내용:**
- nginx upstream에 blue/green 컨테이너 2개 정의
- 배포 스크립트: 새 컨테이너 기동 -> healthcheck 통과 -> nginx upstream 스위칭 -> 구 컨테이너 종료
- `deploy.yml` 또는 별도 스크립트로 자동화

**Acceptance Criteria:**
- [ ] 배포 시 다운타임 0초 (또는 수초 이내)
- [ ] 롤백 시 이전 컨테이너로 즉시 스위칭 가능

---

### P2-2. `/comit-staging` 경로 유지 여부 결정 및 정리

**Why:** staging이 계속 필요한지는 팀 운영 방식에 따라 다르다.

**작업 내용:**
- staging 유지 시: 현행 유지, 추가 작업 없음
- staging 제거 시: nginx에서 `/comit-staging` 제거, staging 컨테이너/DB 정리
- staging 유지하되 격리 강화 시: staging DB를 별도 서버로 분리 검토

**Acceptance Criteria:**
- [ ] 팀 내 결정 문서화
- [ ] 결정에 따른 인프라 변경 완료

---

## Task Flow (권장 실행 순서)

```
Week 1 (Mon-Wed): P0 작업 병렬 수행
  P0-3 DB 준비 ─────────────────────┐
  P0-4 Keycloak 등록 (auth팀 요청) ──┤
  P0-1 application-prod.yml 생성 ────┤──> P0-2 comit.env 작성 ──> P0-5 nginx 설정
                                     │
Week 1 (Thu-Fri): P1 작업           │
  P1-1 PR #59 머지 + 배포 검증 ──────┘
  P1-2 워크플로우 분기 처리
  P1-3 Pinpoint prod 설정

Week 2+: P2 작업 (여유 시 진행)
  P2-1 Blue-Green 스크립트
  P2-2 Staging 정리
```

## Guardrails

### Must Have
- prod에서 `classpath:db/seed` 절대 포함 금지 (dev 데이터가 prod DB에 들어감)
- prod에서 `comit.dev.auth.enabled=false` (dev 로그인 API 노출 차단)
- prod DB 계정은 staging과 분리
- prod/staging 컨테이너명 분리 (deploy.yml에서 잘못된 컨테이너 교체 방지)

### Must NOT Have
- staging 환경 중단 없이 prod 배포 (staging은 현행 유지)
- 아키텍처 변경 (서버 구조, 코드 리팩토링 등은 이번 범위 밖)
- Flyway seed 데이터를 prod에 실행하는 어떤 경로도 없어야 함

## Dependencies / Blockers

| 항목 | 담당 | 상태 |
|------|------|------|
| auth-server redirect URI 등록 | auth-server 팀 | 요청 필요 |
| prod DB 계정 발급 | DB 관리자 | 요청 필요 |
| prod client-id/secret | auth-server 팀 | 요청 필요 |
| PR #59 리뷰 | 백엔드 팀 | 머지 대기 중 |

## Success Criteria

- [ ] `https://chcse.knu.ac.kr/comit/api/actuator/health` 200 OK
- [ ] SSO 로그인 -> callback -> 프론트 리다이렉트 정상
- [ ] prod DB에 seed 데이터 없음 (flyway_schema_history에 V100, V101 없음)
- [ ] DevAuthController가 prod에서 비활성화 (`/auth/dev/login` 404)
- [ ] Pinpoint에서 prod 트래픽 모니터링 가능
- [ ] staging 환경 영향 없음
