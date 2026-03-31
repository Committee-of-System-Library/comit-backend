# ops/

배포·운영 관련 문서. 인프라 변경 또는 운영 이슈 발생 시 업데이트.

## 포함 대상

- 배포 절차
- 환경변수 목록 및 설명
- 운영 체크리스트
- 장애 대응 가이드

## 파일 네이밍

```text
local-development.md ← 로컬 실행 가이드
deploy.md          ← 배포 절차
env.md             ← 환경변수 목록
checklist.md       ← 운영 체크리스트
incident-{내용}.md ← 장애 대응 기록
```

## 현재 문서

| 파일 | 내용 |
|---|---|
| `local-development.md` | 로컬 프로필, Docker MySQL, 임시 인증 헤더 기반 실행 방법 |
| `comit-prod-like-backend-rollout.md` | SSO 미연동 상태에서 `prod-like` 백엔드를 먼저 띄우기 위한 staging 프로필, 임시 인증 브리지, `sidowi` 배포, 검증 순서 계획 |
| `comit-sso-integration-rollout.md` | auth-server custom JWT를 `Comit` backend callback, cookie 인증, `@AuthenticatedMember` 주입으로 연결하기 위한 세부 실행 계획 |
| `comit-staging-verification.md` | `comit-staging` live에서 API docs, CORS, 첫 SSO 로그인 기반 회원 생성까지 실제 검증한 운영 기록과 수동 배포 메모 |
