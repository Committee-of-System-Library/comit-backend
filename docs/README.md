# docs/

프로젝트 문서 루트. 큰 작업이 들어갔을 때는 코드만 바꾸지 말고 이 폴더의 관련 문서도 같이 갱신한다.

## 먼저 볼 문서

- [Comit CPS (왜 만드는지 / 누구를 위한 것인지)](./features/comit-cps.md)
- [게시글·댓글 신고 기능 명세](./features/post-comment-report.md)
- [API 계약 가이드](./guides/api-contract.md)
- [API 문서 생성기 동작 가이드](./guides/api-doc-generator-flow.md)
- [Feature Toggle 설계 가이드](./guides/feature-toggle.md)
- [Java 주석 규칙](./guides/java-comment-convention.md)
- [SSO 인증 흐름 가이드](./guides/sso-auth-flow.md)
- [테스트 전략 가이드](./guides/testing-strategy.md)
- [로컬 실행 가이드](./ops/local-development.md)
- [Comit prod-like 백엔드 롤아웃 계획](./ops/comit-prod-like-backend-rollout.md)
- [Comit SSO 연동 롤아웃 계획](./ops/comit-sso-integration-rollout.md)
- [ADR-002 ProblemDetail 기반 에러 응답 표준 채택](./adr/002-problem-detail-error-response.md)

## 디렉토리 구조

| 폴더 | 용도 |
|---|---|
| `adr/` | 중요한 기술 결정과 대안 비교 |
| `guides/` | 개발자가 따라야 하는 규칙, 작성법, 내부 구조 설명 |
| `features/` | 기능 설계, 전환 계획, 예외 케이스 |
| `ops/` | 로컬 실행, 배포, 환경변수, 운영 체크리스트 |
| `api/` | `./gradlew generateApiDocs`로 생성되는 산출물, `main` 기준 GitHub Pages 배포 대상 |

## 문서 동기화 순서

1. 기준 문서를 먼저 고친다.
   기능 목적/우선순위는 `features/`, 기술 결정은 `adr/`가 원본이다.
2. 개발 가이드를 맞춘다.
   API 계약, 생성기 구조, 테스트 전략, 주석 규칙처럼 개발자가 따라야 하는 문서를 갱신한다.
3. 생성 산출물을 갱신한다.
   API 계약 또는 생성기 변경이면 `./gradlew generateApiDocs`로 `docs/api/`를 재생성한다.
4. 인덱스 README를 마지막에 맞춘다.
   새 문서가 생기거나 문서 성격이 바뀌면 이 파일과 하위 폴더 `README.md`까지 함께 수정한다.

## API 문서 공개 경로

- API 문서는 저장소 안의 `docs/api/`에 커밋된다.
- `main` 브랜치 push 시 GitHub Pages workflow가 같은 산출물을 정적 사이트로 배포한다.
- Pages가 활성화되어 있으면 기본 접근 경로는 `https://<org>.github.io/<repo>/` 이다.

## 변경 유형별 업데이트 대상

| 변경 유형 | 같이 갱신할 문서 |
|---|---|
| 제품 방향, 타겟 사용자, MVP 범위 | `features/comit-cps.md`, `features/README.md`, `docs/README.md` |
| 기능 설계, 흐름, 예외 케이스 | 해당 `features/*.md`, `features/README.md` |
| 에러 응답 포맷, 에러 코드 체계 | `adr/002-problem-detail-error-response.md`, `guides/api-contract.md`, `docs/api/` |
| 컨트롤러 계약, DTO, validation | `guides/api-contract.md`, `docs/api/` |
| API 문서 생성기/어노테이션 규칙 | `guides/api-doc-generator-flow.md`, `docs/api/` |
| 테스트 전략, 테스트 작성 규칙 | `guides/testing-strategy.md`, `guides/README.md`, `docs/README.md` |
| 로컬 실행, 배포, 환경 변수 | `ops/` 하위 문서와 해당 `README.md` |
