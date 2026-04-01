# 댓글 · 대댓글 (Comment & Reply)

## 개요

게시글에 댓글을 달고, 댓글에 대댓글을 달 수 있는 2단계 스레드 구조.
대대댓글(3단계 이상)은 의도적으로 금지한다.
댓글에는 "좋아요" 리액션을 달 수 있고, 좋아요 수가 높은 댓글이 상단에 노출된다.

---

## 도메인 모델

`Comment` 엔티티 하나로 댓글과 대댓글을 모두 표현한다.
대댓글 여부는 `parentComment` 필드의 null 여부로 구분한다.

```
Comment
├── id
├── post           (ManyToOne, LAZY)
├── member         (ManyToOne, LAZY)
├── parentComment  (ManyToOne, LAZY, nullable) ← null이면 댓글, 값이 있으면 대댓글
├── content
├── likeCount
├── createdAt
├── updatedAt
└── deletedAt      ← null이면 활성, 값이 있으면 소프트 삭제
```

`CommentLike` 엔티티는 (comment_id, member_id) unique constraint로 중복 좋아요를 막는다.
`Comment`와 `@ManyToOne` 관계를 맺지 않고 raw `Long`으로 저장한다.
→ 좋아요 여부 조회 시 Comment 엔티티를 불필요하게 로딩하지 않기 위함.

---

## 흐름

### 댓글 / 대댓글 생성

1. 요청에 `parentCommentId`가 없으면 → `Comment.create(post, author, content)` 호출, 최상위 댓글 생성.
2. 요청에 `parentCommentId`가 있으면 → `Comment.reply(post, parentComment, author, content)` 호출.
   - `parentComment`가 이미 대댓글(`isReply() == true`)이면 `INVALID_PARENT_COMMENT` 예외.
   - `parentComment`가 다른 게시글 소속이면 동일하게 `INVALID_PARENT_COMMENT` 예외.
   - 두 검증 모두 도메인 객체 안(`Comment.validateParentComment`)에서 처리한다.

### 댓글 목록 조회

N+1 없이 3번의 쿼리로 전체 스레드를 구성한다.

```
쿼리 1: findActiveTopLevelByPostId   → 최상위 댓글 목록 (JOIN FETCH member, likeCount DESC 정렬)
쿼리 2: findActiveRepliesByPostId    → 해당 게시글의 대댓글 전체 (JOIN FETCH member, JOIN FETCH parentComment)
쿼리 3: findLikedCommentIds          → 현재 요청자가 좋아요 누른 댓글 ID 목록

응답 조립:
  - replies를 parentCommentId 기준으로 Map<Long, List<ReplyResponse>>으로 groupBy
  - 최상위 댓글 순회 시 Map에서 replies를 꺼내 CommentResponse에 포함
  - 댓글/대댓글 응답에는 `likeCount`, `likedByMe`를 함께 내려준다
```

`likedIds`를 `Set<Long>`으로 변환해 `contains()` 조회를 O(1)로 처리한다.

### 댓글 삭제

소프트 삭제를 사용한다 (`deletedAt` 세팅). 물리 삭제 없음.

- 최상위 댓글 삭제 시: 하위 대댓글도 bulk UPDATE 쿼리 한 방으로 함께 소프트 삭제.
  (`softDeleteRepliesByParentCommentId`)
- 대댓글 삭제 시: 해당 대댓글만 소프트 삭제.

### 좋아요 토글

`POST /comments/{commentId}/like`

```
INSERT IGNORE INTO comment_like (comment_id, member_id, created_at) VALUES (?, ?, NOW())
  → affected rows = 1: 최초 좋아요 → incrementLikeCount
  → affected rows = 0: 이미 좋아요 상태 → DELETE + decrementLikeCount
```

`decrementLikeCount` 쿼리에 `AND c.likeCount > 0` 조건을 포함해 음수 방지.

---

## 주요 설계 결정

### 단방향 자기 참조로 대댓글 구현

**결정:** `Comment.parentComment`를 단방향 `@ManyToOne`으로만 유지. `@OneToMany replies` 없음.

**이유:**
양방향 연관관계를 추가하면 `Comment` 로딩 시 replies 컬렉션이 딸려오거나, `@JsonManagedReference`/DTO 변환 없이는 순환 참조가 발생한다.
댓글 목록 조회는 어차피 게시글 단위 전체 조회이므로, 부모-자식 관계를 DB 쿼리 레벨에서 직접 다루는 것이 더 명확하다.

**트레이드오프:** `comment.getReplies()` 같은 도메인 메서드를 쓸 수 없다. 대신 Repository 쿼리로 명시적으로 조회한다.

---

### 대대댓글(3단계) 금지를 도메인에서 강제

**결정:** `Comment.reply()` 팩토리 메서드 내부에서 `parentComment.isReply()`를 검사해 3단계 생성을 막는다.

**이유:**
Controller나 Service에서 막으면 다른 경로로 우회 가능성이 생긴다.
규칙이 도메인 객체 안에 있으면 어느 경로로 생성하든 동일하게 적용된다.

**트레이드오프:** 나중에 3단계를 허용하려면 도메인 로직을 수정해야 한다. 의도적인 설계 고정.

---

### `CommentLike`에 raw Long ID 저장

**결정:** `CommentLike.commentId`, `memberId`를 `@ManyToOne` FK 없이 Long으로 저장한다.

**이유:**
좋아요 조회 패턴은 항상 "이 memberId가 이 commentId들에 좋아요 눌렀는가"이다.
`Comment` 또는 `Member` 엔티티를 로딩할 이유가 없고, 로딩하면 오히려 불필요한 JOIN이 추가된다.

**트레이드오프:** JPA 연관관계 관리 대신 DB unique constraint으로만 무결성을 보장한다. 애플리케이션 레벨의 참조 무결성은 없다.

---

### 대댓글 bulk 소프트 삭제

**결정:** 최상위 댓글 삭제 시 대댓글을 in-memory 루프로 삭제하지 않고, bulk UPDATE 쿼리 한 방으로 처리한다.

**이유:**
대댓글이 많을 경우 모두 메모리에 올린 뒤 N번의 UPDATE를 날리는 것은 불필요한 부하다.
`softDeleteRepliesByParentCommentId(parentId, now)` 한 번으로 처리하면 쿼리가 1개로 고정된다.

**트레이드오프:** bulk UPDATE는 영속성 컨텍스트를 우회하므로 `@Modifying(clearAutomatically = true)`로 1차 캐시를 비워야 한다.

---

## 예외 케이스

| 상황 | 처리 방식 | 에러 코드 |
|---|---|---|
| 존재하지 않는 댓글 조회/수정/삭제 | `COMMENT_NOT_FOUND` | 404 |
| 대대댓글 생성 시도 (부모가 이미 대댓글) | `INVALID_PARENT_COMMENT` | 400 |
| 다른 게시글의 댓글을 부모로 지정 | `INVALID_PARENT_COMMENT` | 400 |
| null 또는 공백 내용으로 댓글 생성/수정 | `INVALID_COMMENT_CONTENT` | 400 |
| 1000자 초과 내용 입력 | validation 실패 (`@Size`) | 400 |
| 본인 댓글이 아닌데 수정/삭제 시도 | `FORBIDDEN` | 403 |
| 삭제된 게시글에 댓글 생성 | `PostService.getActivePostOrThrow` 실패 | 404 |
| 삭제된 부모 댓글의 대댓글은 응답에서 제외 | `findActiveRepliesByPostId`에서 `parent.deletedAt IS NULL` 조건으로 필터 | — |

---

## 관련 ADR

- [ADR-002: ProblemDetail 기반 에러 응답 표준](../adr/002-problem-detail-error-response.md)

---

## 변경 이력

| 날짜 | 변경 내용 |
|---|---|
| 2026-03-25 | 최초 작성 |
