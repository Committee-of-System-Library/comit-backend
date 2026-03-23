# API Contract 사용 가이드

`@ApiContract` 방식은 "문서 정의"와 "컨트롤러 로직"을 분리하기 위한 규칙이다.  
개발자는 인터페이스에 라우팅과 문서만 작성하고, 구현체에는 비즈니스 로직만 남긴다.

관련 배경은 [ADR-001](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/docs/adr/001-api-doc-automation.md)에 정리되어 있다.

## 언제 이 방식을 쓰나

- 새 REST API를 추가할 때
- 기존 컨트롤러를 문서 자동화 구조로 옮길 때
- PR에서 코드 변경과 문서 변경을 같이 추적하고 싶을 때

## 기본 구조

```text
src/main/java/.../payment/
├── controller/
│   ├── api/
│   │   └── PaymentControllerApi.java
│   └── PaymentController.java
├── dto/
└── service/
```

역할은 다음처럼 나눈다.

- `controller/api/*.java`
  - `@ApiContract`
  - `@ApiDoc`
  - `@GetMapping`, `@PostMapping` 등 라우팅 정의
  - 메서드 시그니처 정의
- `controller/*.java`
  - `@RestController`
  - 클래스 레벨 `@RequestMapping`
  - 서비스 호출 등 실제 로직

## 작성 순서

1. `controller/api/` 아래에 인터페이스를 만든다.
2. 인터페이스에 `@ApiContract`를 붙인다.
3. 각 메서드에 `@ApiDoc`과 매핑 어노테이션을 적는다.
4. 구현 컨트롤러는 해당 인터페이스를 `implements` 한다.
5. `./gradlew generateApiDocs`로 문서를 생성한다.

## 무엇을 직접 쓰고 무엇을 자동 추출하나

### 직접 작성하는 항목

- `summary`
- `descriptions`
- `example.request`
- `example.response`

### 자동 추출하는 항목

- HTTP method: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`
- endpoint path: 구현 컨트롤러의 클래스 `@RequestMapping` + 인터페이스 메서드 매핑 path
- path variable: `@PathVariable`
- query param: `@RequestParam`
- request body 필드: `@RequestBody` DTO 리플렉션
- response body 필드: `ResponseEntity<T>`, `ApiResponse<T>`의 제네릭 타입 리플렉션
- required 여부
  - DTO 필드: `@NotNull`, `@NotBlank`, `@NotEmpty`
  - `@RequestParam`: `required = true` 또는 validation 제약

## 예시

### GET API

```java
@ApiContract
public interface PaymentControllerApi {

    @ApiDoc(
        summary = "결제 조회",
        descriptions = {
            @FieldDesc(name = "orderId", value = "조회할 주문 ID"),
            @FieldDesc(name = "includeHistory", value = "거래 이력 포함 여부"),
            @FieldDesc(name = "status", value = "결제 상태")
        }
    )
    @GetMapping("/{orderId}")
    ResponseEntity<PaymentDetailResponse> getPayment(
        @PathVariable("orderId") String orderId,
        @RequestParam(name = "includeHistory", defaultValue = "false") boolean includeHistory
    );
}
```

### POST API

```java
@ApiContract
public interface PaymentControllerApi {

    @ApiDoc(
        summary = "결제 승인",
        descriptions = {
            @FieldDesc(name = "orderId", value = "주문 ID"),
            @FieldDesc(name = "amount", value = "결제 금액"),
            @FieldDesc(name = "paymentKey", value = "결제 키")
        },
        example = @Example(
            request = "{\"orderId\":\"ORDER-001\",\"amount\":15000,\"paymentKey\":\"pay_xxx\"}",
            response = "{\"status\":\"DONE\"}"
        )
    )
    @PostMapping("/confirm")
    ResponseEntity<PaymentConfirmResponse> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request);
}
```

## descriptions 작성 규칙

`descriptions`는 이름으로 매핑된다.  
즉 아래 항목들이 같은 방식으로 연결된다.

- `@PathVariable("orderId")`
- `@RequestParam(name = "includeHistory")`
- request DTO field `paymentKey`
- response DTO field `approvedAt`

예를 들어 `@FieldDesc(name = "orderId", value = "주문 ID")`는 path variable과 body field 둘 중 실제 이름이 같은 곳에 적용된다.

이 때문에 다음 규칙을 지킨다.

- 파라미터 이름과 DTO 필드 이름은 의미 있게 짓는다.
- 의미가 다른 값에 같은 이름을 재사용하지 않는다.
- description 이름은 실제 파라미터/필드 이름과 정확히 맞춘다.

## example 작성 규칙

- `example.request`
  - 보통 `@RequestBody`가 있을 때만 작성
- `example.response`
  - 응답이 복잡하면 가급적 직접 작성
- 비워두면 타입 기반 더미 값이 자동 생성된다

자동 생성 예시 값 규칙

- `String` → `"string"`
- `Integer`, `Long`, `BigDecimal` → `1`
- `Boolean` → `true`
- `LocalDateTime` → `"2024-01-01T12:00:00"`
- enum → 첫 번째 enum 상수

## 문서 생성 방법

```bash
./gradlew generateApiDocs
```

생성 위치

- HTML index: `docs/api/index.html`
- index 데이터: `docs/api/index.js`
- 개별 문서: `docs/api/**/<ControllerApi>.html`

생성 규칙

- 생성 전에 `docs/api/`를 비우고 다시 만든다
- 삭제된 컨트롤러 문서도 같이 사라진다
- `docs/api/` 안 파일은 수동 편집하지 않는다

## CI에서 어떻게 검증되나

PR에서는 다음 순서로 검증한다.

1. `./gradlew generateApiDocs`
2. `git diff --exit-code docs/api`
3. 차이가 있으면 실패

즉 컨트롤러 계약을 바꾸고 문서 산출물을 커밋하지 않으면 PR이 막힌다.

## 권장 설계

- 컨트롤러 경계에서는 항상 DTO/record를 사용한다
- QueryDSL 결과는 서비스나 리포지토리에서 DTO로 변환한다
- 응답 래퍼가 필요하면 `ApiResponse<T>`처럼 명시적 제네릭 래퍼만 사용한다
- `Page<T>`를 직접 반환하기보다 `PageResponse<T>` 같은 DTO로 감싼다

## 비권장 패턴

다음 타입은 자동 문서화 품질이 떨어지므로 API 경계에서 직접 쓰지 않는 것을 권장한다.

- `Tuple`
- `Map<String, Object>`
- `Object`
- `JsonNode`
- `ResponseEntity<?>`

이런 타입은 HTML은 생성되더라도 필드 표와 예시 JSON이 불완전해질 수 있다.

## 변경 체크리스트

새 API를 만들거나 기존 API를 수정할 때 아래 순서대로 본다.

1. 인터페이스에 `@ApiDoc`이 있는가
2. path/query/body 파라미터 이름이 문서 의도와 맞는가
3. response 타입이 DTO로 고정되어 있는가
4. example이 충분히 읽히는가
5. `./gradlew test generateApiDocs`가 통과하는가

## 트러블슈팅

### 문서가 생성되지 않을 때

- 인터페이스에 `@ApiContract`가 있는지 확인
- 구현 컨트롤러가 실제로 해당 인터페이스를 `implements` 하는지 확인
- 메서드에 매핑 어노테이션이 있는지 확인

### path/query 파라미터가 안 보일 때

- `@PathVariable`, `@RequestParam`이 인터페이스 메서드 파라미터에 선언되어 있는지 확인
- description 이름이 실제 파라미터 이름과 같은지 확인

### 필드 표가 비어 있을 때

- 응답 타입이 DTO인지 확인
- `Object`, `Map`, `Tuple` 같은 동적 타입을 쓰고 있지 않은지 확인
- top-level 컬렉션/페이지 타입인지 확인

## 같이 읽으면 좋은 문서

- [ADR-001 API 문서 자동화 방식 선택](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/docs/adr/001-api-doc-automation.md)
- [API 문서 생성기 동작 가이드](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/docs/guides/api-doc-generator-flow.md)
