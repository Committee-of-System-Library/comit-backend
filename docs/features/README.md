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

ProblemDetail 에러 응답 설계는 기능 문서가 아니라 [ADR-002](../adr/002-problem-detail-error-response.md)로 관리한다.

## 템플릿

```markdown
# {기능명}

## 개요
한 줄 설명

## 흐름
1. ...
2. ...

## 예외 케이스
| 상황 | 처리 방식 |
|---|---|
| ... | ... |

## 관련 ADR
- ADR-00X: ...
```
