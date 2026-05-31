# ADR-001. API 문서 자동화 방식 선택

## 상태: 채택

## 날짜: 2026-03-23

---

## 맥락

- Swagger/SpringDoc 어노테이션 인식 불안정, 설정 충돌 빈번
- 팀 내 API 문서 정합성 유지 필요
- 토스페이먼츠 docs 수준의 경량 문서로 충분 (요청 경로·방식·응답 필드·예시)
- 서버 구동 오버헤드 없이 문서 생성 원함

---

## 결정

`@ApiContract` 인터페이스 기반 + Gradle task로 정적 HTML 자동 생성.

### 구조

- `@ApiContract` 인터페이스에 문서 + 라우팅 정의 집중
- Controller는 `implements`만 선언, 순수 비즈니스 로직만 보유
- `./gradlew generateApiDocs` 실행 시 `docs/api/` 하위 HTML 생성
- CI에서 `git diff docs/api/` 검증 → 컨트롤러 변경 시 문서 미업데이트 PR 차단

### 자동 추출 항목 (어노테이션 불필요)

| 항목 | 추출 방법 |
|---|---|
| HTTP method | `@GetMapping` / `@PostMapping` 등 |
| path | 클래스 `@RequestMapping` + 메서드 매핑 조합 |
| request 필드명·타입 | `@RequestBody` 파라미터 DTO 리플렉션 |
| response 필드명·타입 | `ResponseEntity<T>` 제네릭 T 리플렉션 |
| required 여부 | `@NotNull`, `@NotBlank` 등 Bean Validation |

### 개발자 직접 작성 항목

| 항목 | 설명 |
|---|---|
| `summary` | API 한 줄 설명 |
| `descriptions` | 필드별 의미 설명 (생략 시 필드명·타입만 노출) |
| `example` | request/response 예시 JSON (생략 시 타입 기반 더미 자동 생성) |

---

## 검토한 대안

| 대안 | 거절 이유 |
|---|---|
| Swagger/SpringDoc | 어노테이션 인식 불안정, 팀 내 반복 트러블 |
| 수동 문서 관리 | 코드-문서 정합성 보장 불가 |
| 서버 기동 시 문서 생성 | 운영 환경 파일 쓰기 부담, 불필요한 구동 오버헤드 |

---

## 결과

- 컨트롤러 가독성 향상 (문서·로직 분리)
- PR diff에서 문서 변경·로직 변경 명확히 구분 가능
- CI 강제로 문서 최신화 보장
