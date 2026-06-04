# ops/

배포·운영 관련 문서. 인프라 변경 또는 운영 이슈 발생 시 업데이트.

## 포함 대상

- 배포 절차
- 환경변수 목록 및 설명
- 운영 체크리스트
- 장애 대응 가이드

## 파일 네이밍

```text
00–01  인프라 구조 (서버, nginx, runner)
02     로컬 개발 환경
03–05  스테이징 / 운영 설정
06–08  기능별 롤아웃 계획
09+    사고(Incident) 기록
```

## 현재 문서

| 파일 | 내용 |
|---|---|
| `00_sidowi-nginx-proxy-layout.md` | `proxy` 컨테이너가 서빙하는 실제 nginx 설정 경로(`/00_maintenance/0_proxy/`)와 죽은 트리(`/opt/docker/nginx/`) 구분, sudo 없는 root 우회 편집법, conf.d 적용 순서 |
| `01_backend-self-hosted-runner-flow.html` | backend `main` push 이후 GitHub Actions, GHCR, self-hosted runner, `sidowi` compose 재기동이 어떤 순서로 이어지는지 보여주는 미니맵 중심 문서형 HTML |
| `02_local-development.md` | 로컬 프로필, Docker MySQL, 임시 인증 헤더 기반 실행 방법 |
| `03_server-state-and-staging-plan.md` | sidowi 서버 전체 컨테이너 현황, 스테이징 환경 구성 계획 |
| `04_comit-staging-verification.md` | `comit-staging` live에서 API docs, CORS, 첫 SSO 로그인 기반 회원 생성까지 실제 검증한 운영 기록과 수동 배포 메모 |
| `05_load-test-setup-manual.md` | `comit-loadtest` 환경 구성, 부하 테스트 시나리오, 결과 해석 |
| `06_comit-prod-like-backend-rollout.md` | SSO 미연동 상태에서 `prod-like` 백엔드를 먼저 띄우기 위한 staging 프로필, 임시 인증 브리지, `sidowi` 배포, 검증 순서 계획 |
| `07_comit-sso-integration-rollout.md` | auth-server custom JWT를 `Comit` backend callback, cookie 인증, `@AuthenticatedMember` 주입, 2단계 회원가입 경계와 동적 redirectUri 연동까지 포함한 운영 계획 |
| `08_sidowi-pinpoint-rollout.md` | `sidowi`에 Pinpoint를 도입하기 위한 버전 매트릭스, 배치 구조, agent 주입 지점, smoke test, rollback 계획 |
| `09_comit-backend-ai-features-prod-deploy.md` | AI/RAG 기능 운영 배포 시 발생한 3가지 장애(CI Qdrant, Flyway V13 out-of-order, compose env 누락) 원인 분석 및 핫픽스 기록, 정식 수정 TODO |
| `10_comit-naming-and-infra-drift.md` | comit-staging/comit-backend/knu-cse-comit-server 명칭 혼재 원인·전체 지도, core-infra 드리프트 지뢰, URL 경로 교통정리 방향 (3단계 비용 분류) |
| `11_comit-infra-overview.md` | 처음 보는 사람도 이해할 수 있는 인프라 전체 구조 — 요청 흐름·CI/CD·관리 구조 Mermaid 다이어그램 3개, 관리자 플로우 가이드, 현재→목표 상태, 개선 TODO |
| `pinpoint/` | `sidowi` Pinpoint 도입 시 바로 참고할 수 있는 compose/env/JAVA_OPTS 초안 모음 |
