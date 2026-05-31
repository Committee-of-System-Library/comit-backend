# ADR-004: 신고 기능 설계 결정

상태: `채택`

## 맥락

게시글·댓글 신고 기능을 추가하면서 두 가지 설계 결정이 필요했다.

1. 중복 신고 race condition 처리 방식
2. 신고 메시지 길이 검증의 authoritative 위치

---

## 결정 1: 중복 신고 — existsBy + ConstraintViolationException 조합

**흐름**

```
1. existsByReporterIdAndTargetTypeAndTargetId() 로 사전 체크 → 중복이면 즉시 409
2. save() 후 DataIntegrityViolationException 캐치 → race condition 안전망
```

**ConstraintViolationException만 선별 처리**

```java
} catch (DataIntegrityViolationException exception) {
    if (exception.getCause() instanceof ConstraintViolationException) {
        throw new BusinessException(ReportErrorCode.REPORT_ALREADY_EXISTS);
    }
    throw exception;
}
```

**이유**
- `DataIntegrityViolationException` 전체를 409로 흡수하면 FK 위반, null 제약 등 다른 DB 오류도 중복 신고로 오인된다.
- `ConstraintViolationException`(Hibernate)은 unique key 위반에만 발생하므로, cause 타입으로 좁혀서 의미를 정확히 유지한다.

---

## 결정 2: 신고 메시지 검증 — entity가 authoritative

**검증 규칙**: `strip()` 후 1자 이상 500자 이하

**DTO는 `@NotBlank`만 보유, `@Size` 미사용**

```java
public record CreateReportRequest(
        @NotBlank
        String message
) {}
```

**이유**
- `Report.normalizeMessage()`가 `strip()` 후 길이를 검증한다. DTO의 `@Size(max = 500)`은 raw length 기준이라 spec과 어긋난다.
  - 예: `" a" * 250` (500자, 앞뒤 공백 포함) → DTO는 통과, entity는 250자로 strip 후 통과 — 일관성 있음
  - 예: `" " + "a" * 499` (500자) → DTO 통과, entity strip 후 499자 통과 — 일관성 있음
  - 예: `"a" * 501` → DTO 거절(raw 501) vs entity 거절(strip 후 501) — 결과 동일하나 경로 불일치
- `@NotBlank`는 null·공백 문자열을 entity 도달 전에 명확한 Bean Validation 오류로 처리하기 위해 유지한다.
- 길이 상한은 entity의 `MESSAGE_MAX_LENGTH = 500`이 단일 진실 공급원(SSOT)이다.
