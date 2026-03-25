# 테스트 전략 가이드

이 문서는 COMIT 서버에서 테스트를 어디에, 어떤 형태로, 어떤 이름으로 작성할지 정리한 기준이다.

## 목표

- 비즈니스 규칙을 테스트로 먼저 고정한다.
- API 경계는 별도의 웹 테스트로 검증한다.
- 처음 보는 사람도 테스트 이름만 읽고 실패 원인을 짐작할 수 있게 만든다.
- 공개 응답 필드 변경은 웹 테스트와 `docs/api/` 재생성을 같이 확인한다.

## 기본 원칙

- 모든 기능을 엄격한 TDD로 시작할 필요는 없다.
- 대신 변경 비용이 큰 규칙부터 테스트로 고정한다.
- 서비스 테스트는 "무엇을 보장해야 하는가"를 검증한다.
- 웹 테스트는 "요청이 어떻게 들어오고 어떤 응답이 나가는가"를 검증한다.
- 리포지토리 테스트는 QueryDSL, fetch join, cursor, 정렬처럼 DB 의존 로직을 검증한다.

## 테스트 층

### 1. 서비스 테스트

가장 우선순위가 높은 층이다.

- 대상
  - 권한 검사
  - 중복 검사
  - 상태 전이
  - 예외 코드
  - 생성/수정/삭제 규칙
- 예시
  - 닉네임이 중복되면 예외를 던진다.
  - 작성자가 아니면 게시글 수정을 거부한다.
  - 삭제된 댓글은 조회하지 못한다.

### 2. 웹 테스트

컨트롤러 경계와 Spring 바인딩을 검증한다.

- 대상
  - 상태 코드
  - `@RequestBody`, `@PathVariable`, `@RequestParam`
  - validation
  - 인증 헤더/인증 주입
  - 응답 JSON shape

### 3. 리포지토리/통합 테스트

mock으로는 신뢰하기 어려운 DB 쿼리를 검증한다.

- 대상
  - QueryDSL 조건 조합
  - 커서 페이지네이션
  - fetch join / N+1 방지
  - unique constraint와 transaction 경계

## 네이밍 규칙

### 클래스 이름

- `<대상 클래스>Test`
- 예: `MemberServiceTest`, `PostRepositoryTest`

### 구조

- `@Nested`로 공개 메서드 또는 유스케이스 단위를 묶는다.
- `@DisplayName`으로 한글 시나리오를 드러낸다.
- 테스트 메서드 이름은 ASCII로 유지하되, 시나리오가 읽히게 작성한다.

예시

```java
@Nested
@DisplayName("updateNickname")
class UpdateNickname {

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
    void throwsWhenNicknameAlreadyExists() {
    }
}
```

이 방식의 이유는 다음과 같다.

- IDE와 리포트에서는 한글 `@DisplayName`이 바로 읽힌다.
- 메서드명은 ASCII라서 검색, 리팩토링, 호환성이 안정적이다.
- `닉네임_중복_...` 스타일의 가독성은 유지하면서 Java 식별자 제약은 줄일 수 있다.

## 작성 순서

1. 어떤 규칙이 깨지면 가장 위험한지 정한다.
2. 서비스 테스트로 규칙을 먼저 고정한다.
3. 필요하면 웹 테스트로 HTTP 경계를 추가한다.
4. 쿼리가 복잡하면 리포지토리 테스트를 보강한다.

## 테스트 본문 작성 방식

- 한 테스트는 한 시나리오만 다룬다.
- `given / when / then` 흐름이 읽히게 작성한다.
- mock은 직접 협력 객체까지만 둔다.
- 구현 세부사항보다 결과와 부작용을 검증한다.

예시

```java
@Test
@DisplayName("사용 가능한 닉네임이면 회원 닉네임을 수정한다")
void updatesNicknameWhenNicknameIsAvailable() {
    Member member = Member.create("sso-1", "old-name", "20230001");
    given(memberRepository.existsByNickname("new-name")).willReturn(false);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));

    memberService.updateNickname(1L, new UpdateNicknameRequest("new-name"));

    assertThat(member.getNickname()).isEqualTo("new-name");
}
```

## 언제 무엇을 추가하나

- 새 서비스 메서드를 만들면
  - 정상 흐름 1개
  - 핵심 실패 규칙 1~3개
- 새 컨트롤러 엔드포인트를 만들면
  - 성공 응답 1개
  - validation 실패 1개
  - 인증/인가 실패가 있으면 1개
- 새 QueryDSL 쿼리를 만들면
  - 정렬/필터/페이지네이션 조합 검증

## 현재 프로젝트 권장 우선순위

1. `MemberServiceTest`
2. `PostServiceTest`
3. `CommentServiceTest`
4. QueryDSL/리포지토리 테스트

## 체크리스트

- 테스트 이름만 읽고 의도가 보이는가
- 실패 시 어떤 규칙이 깨졌는지 바로 알 수 있는가
- 서비스 테스트가 웹 테스트보다 먼저 존재하는가
- mock이 너무 많아서 구현 따라 쓰기가 되지 않았는가
- 상태 코드보다 에러 코드와 도메인 규칙을 먼저 검증하고 있는가
