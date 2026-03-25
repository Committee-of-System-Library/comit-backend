# Git 컨벤션

## 브랜치 전략

```text
main          ← 배포 브랜치
└── feat/#이슈번호     ← 기능 개발
└── fix/#이슈번호      ← 버그 수정
└── refactor/#이슈번호 ← 리팩터링
└── docs/#이슈번호     ← 문서 작업
└── chore/#이슈번호    ← 빌드·설정 변경
```

- 브랜치는 반드시 GitHub 이슈 번호와 연결
- `main` 직접 푸시 금지, PR을 통해서만 머지

---

## 커밋 메시지

Conventional Commits 형식 준수.

```text
<type>: <subject>

[optional body]
```

### type 목록

| type | 사용 시점 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `docs` | 문서 추가·수정 (AGENTS.md, ADR, API 문서 등) |
| `test` | 테스트 추가·수정 |
| `chore` | 빌드 설정, 의존성 변경 |

### 예시

```text
feat: 결제 승인 API 구현

POST /v1/payments/confirm 엔드포인트 추가.
PaymentController, PaymentService, PaymentRepository 포함.
```

```text
docs: API 문서 재생성

PaymentControllerApi 변경에 따른 docs/api 업데이트.
```

---

## PR 규칙

- 제목: 커밋 메시지 형식과 동일 (`feat: ...`)
- 본문: 변경 이유 + 주요 변경 사항 요약
- 컨트롤러 변경 시 `./gradlew generateApiDocs` 실행 후 커밋 필수
- CI 통과 후 머지 (API 문서 diff 검증 포함)
- 최소 1인 리뷰 승인 후 머지
