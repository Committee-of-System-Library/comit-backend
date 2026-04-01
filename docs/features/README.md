# features/

기능별 설계 문서. 기능을 구현하기 전 또는 구현 중에 작성.

## 작성 대상

- 도메인 흐름이 복잡한 기능
- 예외 케이스가 많은 기능
- 팀원 간 합의가 필요한 설계

## 파일 네이밍

```text
{기능명}.md
예) payment-confirm.md, member-auth.md
```

## 현재 문서

- [comit-cps.md](./comit-cps.md)
  Comit의 타겟 사용자, 해결하려는 문제, MVP 범위와 우선순위 판단 기준
- [comment-reply.md](./comment-reply.md)
  댓글·대댓글 2단계 스레드 구조, 도움이요 토글, 설계 결정 및 트레이드오프
- [feature-spec-template.md](./feature-spec-template.md)
  새 기능 설계를 시작할 때 복사해서 사용하는 기능 명세 템플릿
- [post-comment-report.md](./post-comment-report.md)
  게시글·댓글 신고 접수, 자유 입력 사유, 중복 신고 방지, v1 범위 정의
- [sso-registration-flow.md](./sso-registration-flow.md)
  SSO 로그인, 2단계 회원가입, callback stage 분기, 동적 redirectUri, soft delete 회원 예외 흐름 정의

ProblemDetail 에러 응답 설계는 기능 문서가 아니라 [ADR-002](../adr/002-problem-detail-error-response.md)로 관리한다.

## 템플릿

새 기능은 [feature-spec-template.md](./feature-spec-template.md)를 복사해 작성한다.
