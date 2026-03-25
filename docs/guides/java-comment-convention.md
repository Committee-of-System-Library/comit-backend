# java-comment-convention

Java 코드 주석 규칙. 읽는 사람이 "왜 이 코드가 이렇게 생겼는지"를 빠르게 이해할 수 있게 만드는 것이 목적이다.

## 기본 원칙
- `TODO`, `FIXME`, 섹션 구분용 `// =====` 주석은 남기지 않는다.
- 공개 타입과 공개 메서드 설명은 일반 line comment 대신 Javadoc으로 남긴다.
- 비즈니스 규칙은 코드와 테스트로 드러내고, 구현 제약이나 임시 브리지 설명만 문서화한다.
- private helper에는 정말 필요한 경우에만 짧은 설명을 남긴다.

## 어떤 태그를 쓰나
- `@apiNote`: 외부에서 이 타입이나 메서드를 어떻게 이해해야 하는지 설명
- `@implNote`: 현재 구현 선택, 제약, 임시 브리지, 교체 예정 포인트 설명
- `@return`, `@param`, `@throws`: 시그니처만으로 바로 안 읽히는 경우에만 보강

## 권장 패턴
```java
/**
 * Read posts with cursor pagination.
 *
 * @apiNote Passing {@code null} as the cursor reads the first page.
 * @implNote The repository fetches authors eagerly and the service fills
 * comment counts separately so the main flow still reads top-down.
 */
public PostCursorPageResponse getPosts(BoardType boardType, Long cursorId, int size) {
    ...
}
```

## `TODO`를 대체하는 방법
- 정말 당장 남겨야 할 과제라면 이슈 번호와 함께 기능 문서에 적는다.
- 코드에는 `@implNote`로 "현재 왜 이렇게 구현됐는지"만 남긴다.
- 교체 시점과 대상 구조는 `docs/features/*.md` 또는 `docs/adr/*.md`에 기록한다.

## 적용 기준
- Controller, Service, Repository의 공개 메서드
- 임시 어댑터나 브리지 성격이 강한 클래스
- 도메인 규칙을 담은 정적 팩토리나 상태 변경 메서드

## 피해야 하는 형태
```java
// TODO: 나중에 SSO 붙이면 바꾸기
// 좋아요 토글
// ===== query =====
```
