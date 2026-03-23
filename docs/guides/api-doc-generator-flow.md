# API 문서 생성기 동작 가이드

이 문서는 "왜 이 코드가 이렇게 동작하는가"와 "나중에 지원 범위를 바꾸려면 어디를 수정해야 하는가"를 설명한다.

대상 독자

- 처음 이 구조를 유지보수하는 사람
- 지원 타입을 추가하려는 사람
- 문서 생성 결과가 왜 이렇게 나오는지 추적해야 하는 사람

## 전체 흐름

```text
./gradlew generateApiDocs
    ↓
ApiDocGenerator.main()
    ↓
ApiDocIntrospector.inspect()
    ↓
@ApiContract 인터페이스 스캔
    ↓
구현 컨트롤러 매칭
    ↓
요청/응답/예시/설명 추출
    ↓
GeneratedApiDocument 모델 생성
    ↓
ApiDocHtmlRenderer로 HTML + index.js 렌더링
    ↓
docs/api/ 전체 재생성
```

## 주요 코드 위치

- 엔트리포인트
  - [ApiDocGenerator.java](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/src/main/java/kr/ac/knu/comit/docs/ApiDocGenerator.java)
- 메타데이터 추출
  - [ApiDocIntrospector.java](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/src/main/java/kr/ac/knu/comit/docs/ApiDocIntrospector.java)
- 렌더링
  - [ApiDocHtmlRenderer.java](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/src/main/java/kr/ac/knu/comit/docs/ApiDocHtmlRenderer.java)
- 중간 모델
  - [ApiDocModels.java](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/src/main/java/kr/ac/knu/comit/docs/ApiDocModels.java)

## 1. Gradle task 진입

`build.gradle`의 `generateApiDocs` task가 `JavaExec`로 생성기를 실행한다.

입력값

- base package: `kr.ac.knu.comit`
- output directory: `docs/api`

출력값

- `docs/api/index.html`
- `docs/api/index.js`
- `docs/api/**/<ControllerApi>.html`

## 2. 계약 인터페이스 스캔

`ApiDocIntrospector.inspect()`는 두 가지를 찾는다.

- `@ApiContract`가 붙은 인터페이스
- `@RestController` 또는 `@Controller`가 붙은 구현 클래스

이후 각 계약 인터페이스마다 구현 컨트롤러를 하나만 매칭한다.

현재 규칙

- 구현체가 없으면 실패
- 구현체가 둘 이상이면 실패

즉 문서 대상 API는 "계약 1개 : 구현 1개" 관계를 전제로 한다.

## 3. endpoint 메타데이터 추출

각 인터페이스 메서드마다 다음 순서로 정보를 만든다.

### 3-1. 문서 어노테이션 확인

- `@ApiDoc`가 없으면 실패
- summary, descriptions, example을 읽는다

### 3-2. HTTP method와 path 추출

- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@PatchMapping`
- `@DeleteMapping`
- 필요 시 `@RequestMapping`

최종 path는 아래 규칙으로 조합한다.

- 클래스 `@RequestMapping`
- 메서드 매핑 path

예시

```text
클래스: /v1/payments
메서드: /confirm
최종: /v1/payments/confirm
```

### 3-3. 요청 정보 추출

현재 지원 범위

- `@PathVariable`
- `@RequestParam`
- `@RequestBody`

출력 모델은 세 갈래로 나뉜다.

- path parameters
- query parameters
- request body fields

이렇게 분리한 이유

- URL 변수와 body field를 같은 표에 섞으면 API 읽기가 어려워진다
- 유지보수 시 파라미터 추출 로직을 구분하기 쉽다

### 3-4. 응답 정보 추출

현재는 다음 래퍼를 자동 언랩한다.

- `ResponseEntity<T>`
- `ApiResponse<T>`

언랩 후의 최종 타입을 기준으로 response body field를 만든다.

## 4. 필드 추출 규칙

DTO 필드는 리플렉션으로 읽는다.

현재 규칙

- static 필드 제외
- synthetic 필드 제외
- 상속 필드 포함
- 필드명 기준 정렬

required 판단 규칙

- `@NotNull`
- `@NotBlank`
- `@NotEmpty`

`@RequestParam`은 다음 우선순위로 판단한다.

1. `defaultValue`가 있으면 optional
2. validation 제약이 있으면 required
3. 아니면 `required` 속성 사용

## 5. 예시 JSON 생성 규칙

개발자가 직접 예시를 적으면 그 값을 우선 사용한다.  
비워두면 타입 기반으로 자동 생성한다.

대표 규칙

- `String` → `"string"`
- 숫자형 → `1`
- `Boolean` → `true`
- `LocalDate` → `"2024-01-01"`
- `LocalDateTime` → `"2024-01-01T12:00:00"`
- enum → 첫 상수
- `Collection<T>` → `[example(T)]`
- `Map<K, V>` → `{ "key": example(V) }`

## 6. HTML 렌더링 규칙

렌더러는 문서를 아래 블록으로 고정한다.

- endpoint 요약
- 요청 경로 / method
- 경로 변수
- 쿼리 파라미터
- 요청 바디 필드
- 응답 바디 필드
- 요청 예시
- 응답 예시

빈 섹션은 출력하지 않는다.  
즉 GET API에 body가 없으면 `요청 바디 필드` 섹션 자체가 나오지 않는다.

## 7. 일관성을 보장하는 장치

현재 출력은 가능한 한 결정적으로 만든다.

- 계약 인터페이스 정렬: 클래스명 기준
- 메서드 정렬: 메서드명 기준
- 필드 정렬: 필드명 기준
- 산출 폴더 재생성: 기존 `docs/api` 삭제 후 전체 생성

이 규칙 덕분에 같은 입력에서 같은 HTML/JS가 나와 `git diff`가 안정적으로 유지된다.

## 8. 현재 지원 범위

### 자동 지원

- `@ApiContract`
- `@ApiDoc`
- `@PathVariable`
- `@RequestParam`
- `@RequestBody`
- `ResponseEntity<T>`
- `ApiResponse<T>`
- DTO 필드
- Bean Validation required 추론

### 제한적으로 지원

- 중첩 DTO 예시 자동 생성
- 컬렉션 타입 예시 자동 생성
- `Map<K, V>` 예시 자동 생성

### 비권장 또는 미지원

- QueryDSL `Tuple`
- `Map<String, Object>`를 실제 응답 스키마로 사용하는 경우
- `Object`
- `JsonNode`
- wildcard generic
- top-level `Page<T>`, `Slice<T>` 직접 노출

이 타입들은 "문서는 생성돼도 정확한 스키마를 보장하지 못할 수 있다".

## 9. 지원 범위를 늘릴 때 어디를 고치나

### 새 요청 어노테이션 지원 예: `@RequestHeader`

수정 포인트

1. `ApiDocModels.java`
   - endpoint 모델에 header 섹션 추가 여부 결정
2. `ApiDocIntrospector.java`
   - 파라미터 추출 로직에 `@RequestHeader` 분기 추가
3. `ApiDocHtmlRenderer.java`
   - 새 섹션 렌더링 추가
4. 테스트
   - web test
   - generator test

### 새 응답 래퍼 지원 예: `CommonResponse<T>`

수정 포인트

1. `ApiDocIntrospector.unwrapDocumentType()`
   - 언랩 대상에 `CommonResponse.class` 추가
2. generator test
   - 언랩 결과가 필드 표에 반영되는지 검증

### `Page<T>` 지원을 넣고 싶을 때

권장 방식은 둘 중 하나다.

1. `PageResponse<T>` 같은 DTO를 별도로 만든다
2. 정말 자동 지원이 필요하면 `Page<T>`를 별도 규칙으로 펼친다

현재는 1번을 권장한다. 2번은 문서 형식이 프레임워크 세부 구조에 강하게 묶이기 때문이다.

## 10. 변경 시 주의할 점

- 문서 생성 규칙은 되도록 "명시적"이어야 한다
- 잘못된 자동화보다 제한된 자동화가 낫다
- API 경계에서 동적 타입을 허용하면 문서 품질이 빠르게 떨어진다
- HTML 템플릿 변경은 모든 문서 diff에 영향을 준다
- index 정렬 규칙을 바꾸면 PR diff가 크게 출렁일 수 있다

## 11. 추천 테스트 전략

변경 후에는 최소 아래를 돌린다.

```bash
./gradlew test generateApiDocs
```

확인 포인트

- web test: 인터페이스에 선언한 어노테이션이 실제 런타임 매핑에 반영되는가
- generator test: 생성된 HTML에 새 섹션/타입/경로가 들어가는가
- generated docs: `docs/api` diff가 의도한 내용만 바뀌는가

## 12. 나중에 문서가 이상할 때 디버깅 순서

1. 인터페이스에 `@ApiContract`, `@ApiDoc`가 있는가
2. 구현 컨트롤러가 인터페이스를 `implements` 하는가
3. path/query/body 어노테이션이 인터페이스 메서드에 붙어 있는가
4. response 타입이 DTO인지 동적 타입인지 확인했는가
5. `ApiDocIntrospector`에서 어떤 타입으로 인식했는가
6. `ApiDocHtmlRenderer`가 그 모델을 어떤 섹션으로 렌더링했는가

## 같이 보면 좋은 문서

- [API Contract 사용 가이드](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/docs/guides/api-contract.md)
- [ADR-001 API 문서 자동화 방식 선택](/Users/bohyeong/IdeaProjects/knu-cse-comit-server/docs/adr/001-api-doc-automation.md)
