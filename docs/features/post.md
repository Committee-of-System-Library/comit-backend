# 게시글 (Post)

## 1. Overview

### 1.1 Goal
- KNU CSE 학생들이 QNA·FREE 게시판에 글을 작성하고 조회·수정·삭제할 수 있어야 한다.
- 태그, 좋아요, 조회수를 통해 게시글을 분류하고 인기도를 측정한다.
- 최근 활동 기반 Hot Post 5개를 별도로 제공한다.

### 1.2 In Scope
- 게시글 CRUD (제목·본문·태그)
- 게시판 타입별 커서 기반 목록 조회
- 좋아요 토글
- 조회수 증가
- Hot Post 조회
- 소프트 삭제

### 1.3 Out of Scope
- 이미지 첨부 (별도 구현 예정)
- 게시글 검색
- 고정 게시글(공지)
- 임시저장

### 1.4 Success Signal
- 로그인한 사용자가 QNA·FREE 게시판에 글을 작성·수정·삭제할 수 있다.
- 게시판 목록이 커서 기반으로 무한 스크롤 된다.
- 좋아요·조회수가 실시간으로 반영된다.
- Hot Post 5개가 최근 7일 활동 기준으로 노출된다.

---

## 2. Domain Context

### 2.1 도메인 모델

```
Post
├── id
├── member          (ManyToOne, LAZY) ← 작성자
├── boardType       (QNA | FREE)
├── title           (최대 30자)
├── content         (최대 500자)
├── likeCount       (정규화 카운터 — PostLike 개수와 동기화)
├── viewCount       (조회수)
├── tags            (List<PostTag>, 최대 5개)
├── createdAt
├── updatedAt
└── deletedAt       ← null이면 활성, 값이 있으면 소프트 삭제
```

```
PostTag
├── post    (ManyToOne)
└── name    (태그명, 소문자 정규화)
```

```
PostLike
├── postId    (Long, raw)
├── memberId  (Long, raw)
└── (postId, memberId) UNIQUE constraint
```

```
PostDailyVisitor
├── postId
├── memberId
└── viewedOn  (LocalDate)  ← 당일 중복 조회 방지
```

### 2.2 BoardType

| 값 | 설명 |
|----|------|
| `QNA` | 메인 Q&A 게시판 |
| `FREE` | 일반 자유 게시판 |

### 2.3 Constraints

| 항목 | 제한 |
|------|------|
| 제목 | 1 ~ 30자 |
| 본문 | 1 ~ 500자 |
| 태그 | 최대 5개 |

### 2.4 Assumptions
- 게시글 작성·수정·삭제는 로그인 필수다.
- 조회는 비로그인도 가능하다. 단 좋아요 여부(`liked`)는 로그인 시에만 반환된다.
- `likeCount`는 `PostLike` 테이블과 별도로 `Post` 엔티티에 정규화 컬럼으로 관리한다.
- 태그명은 저장 시 소문자로 정규화한다.

---

## 3. Scenarios

### Scenario A. 게시글 작성
Given: 로그인한 사용자가 제목·본문·태그를 입력한다.
When: `POST /posts`
Then: 게시글이 생성된다. 태그가 있으면 `PostTag`가 함께 저장된다.

### Scenario B. 게시판 목록 조회 (커서 페이지네이션)
Given: 특정 게시판(boardType)을 선택한다.
When: `GET /posts?boardType=QNA&size=10` (첫 페이지) 또는 `GET /posts?boardType=QNA&cursor=42&size=10`
Then: 최신순으로 최대 `size`개를 반환한다. 다음 페이지가 있으면 `nextCursor`를 포함한다.

### Scenario C. 게시글 상세 조회
Given: 특정 게시글 ID로 요청한다.
When: `GET /posts/{postId}`
Then: 게시글 상세가 반환된다. 조회수가 1 증가한다. 로그인 사용자면 `liked` 여부를 포함한다.

### Scenario D. 좋아요 토글
Given: 로그인한 사용자가 게시글을 본다.
When: `POST /posts/{postId}/like`
Then:
- 좋아요가 없으면 → `PostLike` 삽입, `likeCount + 1`
- 좋아요가 있으면 → `PostLike` 삭제, `likeCount - 1`

### Scenario E. 게시글 수정
Given: 로그인한 사용자가 자신이 쓴 게시글을 수정한다.
When: `PATCH /posts/{postId}`
Then: 제목·본문·태그가 업데이트된다. 태그는 전체 교체된다.

### Scenario F. 게시글 삭제
Given: 로그인한 사용자가 자신이 쓴 게시글을 삭제한다.
When: `DELETE /posts/{postId}`
Then: `deletedAt`이 설정된다 (소프트 삭제). 이후 목록·상세 조회에서 노출되지 않는다.

### Scenario G. Hot Post 조회
Given: 메인 화면에서 Hot Post를 요청한다.
When: `GET /posts/hot`
Then: 최근 7일 기준 점수 상위 5개 게시글이 반환된다.

---

## 4. Functional Requirements

- FR-1: `POST /posts`는 `boardType`, `title`, `content`를 필수로 받는다. `tags`는 선택이다.
- FR-2: 제목은 1~30자, 본문은 1~500자를 초과하면 `400 INVALID_POST_TITLE` / `400 INVALID_POST_CONTENT` 예외.
- FR-3: 태그는 최대 5개. 초과 시 `400 INVALID_TAG` 예외.
- FR-4: 목록 조회는 커서 기반 페이지네이션. `cursor`가 없으면 첫 페이지.
- FR-5: 상세 조회 시 조회수를 1 증가시킨 뒤 최신 상태로 응답한다.
- FR-6: 좋아요는 동일 사용자가 같은 게시글에 중복 삽입되지 않는다 (DB UNIQUE constraint).
- FR-7: 수정·삭제는 작성자 본인만 가능하다. 타인이 시도하면 `403 FORBIDDEN`.
- FR-8: 삭제된 게시글 조회 시 `404 POST_NOT_FOUND`.
- FR-9: `GET /posts/hot`은 최근 7일 활동 기준 상위 5개를 반환한다. 활성 게시글이 없으면 빈 배열.

---

## 5. Behavioral Rules

### 5.1 Hot Post 점수 계산

```
score = (최근 7일 좋아요 수 × 5) + (최근 7일 댓글 수 × 3) + (최근 7일 방문자 수 × 1)
```

- 동점이면 `created_at DESC`, `id DESC` 순서로 정렬한다.
- 소프트 삭제된 게시글은 Hot Post 대상에서 제외된다.

### 5.2 조회수 중복 방지
- `PostDailyVisitor`(postId, memberId, viewedOn) 기준으로 당일 동일 사용자의 중복 조회는 `viewCount`를 증가시키지 않는다.
- 비로그인 사용자는 중복 방지 없이 매 조회마다 `viewCount`가 증가한다.

### 5.3 좋아요 원자성
- `PostLike` 삽입 시 `INSERT IGNORE`를 사용해 concurrent 중복 요청을 안전하게 처리한다.
- `likeCount`는 `UPDATE ... SET likeCount = likeCount + 1` 방식으로 원자적으로 업데이트한다.

### 5.4 태그 정규화
- 태그명은 저장 시 소문자로 변환한다.
- 수정 시 기존 태그를 전체 삭제 후 새 태그로 교체한다.

### 5.5 Forbidden Rules
- 삭제된 게시글을 수정하거나 좋아요하는 것
- 타인의 게시글을 수정·삭제하는 것
- `likeCount`를 0 미만으로 감소시키는 것

---

## 6. External Contracts

### API 요약

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| `GET` | `/posts` | 선택 | 게시판 목록 (커서) |
| `GET` | `/posts/hot` | 불필요 | Hot Post 5개 |
| `GET` | `/posts/{postId}` | 선택 | 상세 조회 |
| `POST` | `/posts` | 필수 | 작성 |
| `PATCH` | `/posts/{postId}` | 필수 | 수정 |
| `DELETE` | `/posts/{postId}` | 필수 | 삭제 |
| `POST` | `/posts/{postId}/like` | 필수 | 좋아요 토글 |
| `POST` | `/posts/{postId}/reports` | 필수 | 신고 |

---

## 7. Non-Functional Constraints

- 목록 조회 시 N+1 방지: `JOIN FETCH member` 사용.
- `likeCount` 정규화 컬럼으로 집계 쿼리 없이 빠른 조회.
- Hot Post는 `findHotPostScores` 단일 쿼리로 점수 계산 후 `findActiveWithMemberAndTagsByIds`로 배치 로딩.
