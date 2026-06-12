# ops/

배포·운영 관련 문서. 인프라 변경 또는 운영 이슈 발생 시 업데이트.

## 포함 대상

- 배포 절차
- 환경변수 목록 및 설명
- 운영 체크리스트
- 장애 대응 가이드

## 파일 네이밍

실제 파일명은 `{대상}-{목적}.md` 패턴을 따른다.

```text
local-development.md          ← 로컬 실행 가이드
{서비스}-rollout.md            ← 특정 기능·환경의 도입 계획
{서비스}-verification.md       ← 실제 검증 기록
{서비스}-implementation-log.md ← 적용 과정 트러블슈팅 기록
{서비스}-deployment.md         ← 배포 계획 및 체크리스트
incident-{내용}.md             ← 장애 대응 기록
```

## 현재 문서

### 로컬 개발

| 파일 | 내용 |
|---|---|
| `local-development.md` | 로컬 프로필, Docker MySQL, 임시 인증 헤더 기반 실행 방법 |

### Comit 배포·운영

| 파일 | 상태 | 내용 |
|---|---|---|
| `comit-prod-deployment.md` | 진행 중 | prod 배포 P0/P1/P2 우선순위 플랜, 다음주 배포 기준 |
| `comit-prod-like-backend-rollout.md` | 대부분 완료 | staging 환경 구성 계획 — 체크리스트 현행 기준으로 갱신됨 |
| `comit-sso-integration-rollout.md` | 완료 (T1~T7) | auth-server custom JWT 연동, 2단계 회원가입, cookie 인증 구현 계획 |
| `comit-staging-verification.md` | 완료 | staging live 검증 기록 (API docs, CORS, SSO 로그인, healthcheck 이슈) |

### Pinpoint APM

| 파일 | 상태 | 내용 |
|---|---|---|
| `sidowi-pinpoint-rollout.md` | 대부분 완료 | Pinpoint 도입 계획, 버전 매트릭스, agent 주입 지점 |
| `sidowi-pinpoint-implementation-log.md` | 완료 | UI white screen, Inspector 404/500, Kafka·Pinot 추가, 메모리 튜닝, TLS 확인까지 실행 기록 |
| `pinpoint/` | 참고용 | compose/env/JAVA_OPTS 초안 모음 |

### 인프라 참고

| 파일 | 내용 |
|---|---|
| `backend-self-hosted-runner-flow.html` | `main` push → GitHub Actions → GHCR → self-hosted runner → sidowi compose 재기동 흐름 시각화 |
