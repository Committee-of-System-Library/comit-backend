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
- 가능하면 `// given`, `// when`, `// then` 주석을 본문에 명시한다.
- 각 섹션 바로 아래에는 "무엇을 준비하는지 / 무엇을 실행하는지 / 무엇을 검증하는지"를 설명하는 한 줄짜리 한글 주석을 둔다.
- mock은 직접 협력 객체까지만 둔다.
- 구현 세부사항보다 결과와 부작용을 검증한다.

예시

```java
@Test
@DisplayName("사용 가능한 닉네임이면 회원 닉네임을 수정한다")
void updatesNicknameWhenNicknameIsAvailable() {
    // given
    // 닉네임 변경이 가능한 회원 상태를 준비한다.
    Member member = Member.create(
        "sso-1",
        "테스트유저",
        "010-0000-0000",
        "old-name",
        "20230001",
        null,
        LocalDateTime.now()
    );
    given(memberRepository.existsByNickname("new-name")).willReturn(false);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));

    // when
    // 닉네임 변경을 실행한다.
    memberService.updateNickname(1L, new UpdateNicknameRequest("new-name"));

    // then
    // 회원 닉네임이 새 값으로 바뀌어야 한다.
    assertThat(member.getNickname()).isEqualTo("new-name");
}
```

## Fixture 사용 기준

- 테스트 데이터의 본질이 아닌 생성 보일러플레이트를 줄이기 위해 fixture helper를 우선 사용한다.
- fixture는 `src/test/java/.../fixture/` 아래에 두고, 기본값이 많은 도메인 객체 생성에만 사용한다.
- fixture는 "읽기 쉬운 기본 상태"를 제공하고, 개별 시나리오 차이는 테스트 본문에서 드러나게 한다.
- Reflection 기반 필드 주입이 필요한 값은 fixture 안으로 숨겨 테스트 본문에서 반복하지 않는다.
- 단, 시나리오의 핵심 의미가 fixture 안에 가려지면 테스트 본문에서 직접 생성한다.

예시

```java
Post post = PostFixture.post(10L);
Member author = MemberFixture.member(1L, "writer");
Comment parent = CommentFixture.topLevelComment(201L, post, author, "부모 댓글", 0);
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

## 현재 커버리지 갭 (2026-03-28 기준)

아래 시나리오는 구현은 있으나 서비스 테스트가 없는 상태다.
새 기능 작업 전에 이 갭부터 채우는 것을 권장한다.

### PostService

| 메서드 | 추가해야 할 시나리오 |
|---|---|
| `createPost` | 정상 생성 후 postId 반환 |
| `updatePost` | 정상 수정, 작성자 아니면 FORBIDDEN |
| `deletePost` | 정상 삭제, 작성자 아니면 FORBIDDEN |
| `forceDeletePost` | 관리자가 타인 게시글 강제 삭제 |
| `toggleLike` | 첫 좋아요 → liked / 이미 좋아요 → unliked / POST_NOT_FOUND |
| `getPosts` | `size <= 0` → INVALID_REQUEST, `size > 20` → 20으로 cap |

### CommentService

| 메서드 | 추가해야 할 시나리오 |
|---|---|
| `updateComment` | 정상 수정, 작성자 아니면 FORBIDDEN |
| `deleteComment` | 대댓글 삭제 시 `softDeleteReplies` 미호출, 작성자 아니면 FORBIDDEN |

## 서비스 설계 원칙 — 권한 플래그 안티패턴

서비스 메서드에 `boolean admin` 같은 권한 플래그를 파라미터로 넘기지 않는다.

**나쁜 예**
```java
// boolean이 늘어날수록 시그니처가 깨지고 테스트가 어색해진다
public void deletePost(Long memberId, boolean admin, Long postId)
```

**좋은 예 — 유스케이스 단위로 메서드를 분리한다**
```java
// 작성자 삭제: 소유권 검사 포함
public void deletePost(Long memberId, Long postId)

// 관리자 강제 삭제: 소유권 검사 없음
public void forceDeletePost(Long postId)
```

컨트롤러가 `principal.isAdmin()`을 보고 어느 메서드를 호출할지 결정한다.
서비스는 "무엇을 하는가"만 책임지고, "누가 할 수 있는가"는 컨트롤러 레이어에서 분기한다.

이 원칙을 지키면 테스트가 자연스럽게 단순해진다.
`deletePost` 테스트는 소유권 규칙만, `forceDeletePost` 테스트는 존재 여부만 검증하면 된다.

## 체크리스트

- 테스트 이름만 읽고 의도가 보이는가
- 실패 시 어떤 규칙이 깨졌는지 바로 알 수 있는가
- 서비스 테스트가 웹 테스트보다 먼저 존재하는가
- mock이 너무 많아서 구현 따라 쓰기가 되지 않았는가
- 상태 코드보다 에러 코드와 도메인 규칙을 먼저 검증하고 있는가
- fixture가 반복 보일러플레이트를 줄이고 있는가
- `// given`, `// when`, `// then`과 한 줄 한글 설명이 본문 흐름을 명확하게 만드는가

## AI / 계획 문서와의 관계

- AI가 리팩토링 계획이나 테스트 계획을 제안할 때, 저장소 전용 테스트 규칙은 이 문서를 기준으로 잡는다.
- 특히 테스트 층 선택, fixture 사용 여부, `given / when / then` 본문 스타일은 이 문서를 우선 참조한다.
- 외부 skill이나 개인 프롬프트는 이 문서를 대체하지 않고, 이 문서를 참조하는 연결 레이어로만 사용한다.
