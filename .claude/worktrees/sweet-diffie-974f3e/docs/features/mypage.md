# 마이페이지 (My Page)

## 1. Overview

### 1.1 Goal
- 로그인한 사용자가 자신의 프로필을 조회·편집하고, 활동 내역을 한눈에 볼 수 있어야 한다.
- 내가 쓴 글 / 내가 쓴 댓글 / 좋아요한 글을 미리보기(최신 3개) + 전체 목록(커서 페이지네이션)으로 제공한다.

### 1.2 In Scope
- 프로필 조회 (`GET /members/me`)
- 닉네임 변경 (`PATCH /members/me`)
- 학번 공개 여부 설정 (`PATCH /members/me/student-number-visibility`)
- 내가 쓴 글 목록 (`GET /members/me/posts`)
- 내가 쓴 댓글 목록 (`GET /members/me/comments`)
- 좋아요한 글 목록 (`GET /members/me/likes`)
- 로그아웃 (`POST /auth/sso/logout`)

### 1.3 Out of Scope
- 프로필 이미지 변경
- 회원 탈퇴
- 이름·학번·전공 변경 (SSO에서 읽어온 값으로 고정)
- 활동 통계 (기간별 집계 등)

### 1.4 Success Signal
- 로그인한 사용자가 자신의 프로필 정보를 볼 수 있다.
- 내가 쓴 글·댓글·좋아요 목록이 최신순으로 커서 페이지네이션된다.
- 삭제된 게시글·댓글은 목록에서 제외된다.

---

## 2. Domain Context

### 2.1 프로필 정보

| 항목 | 출처 | 변경 가능 |
|------|------|----------|
| 이름 | SSO JWT | ❌ |
| 학번 | SSO JWT | ❌ |
| 전공 | SSO JWT | ❌ |
| 닉네임 | 회원가입 입력 | ✅ |
| 학번 공개 여부 | 설정 | ✅ |

### 2.2 Assumptions
- 마이페이지 3종 API는 모두 로그인 필수다. 미인증 시 `401 UNAUTHORIZED`.
- 삭제된 게시글에 달린 댓글도 내 댓글 목록에서 제외한다.
- 좋아요 목록은 내가 좋아요한 시점 기준 최신순이다 (PostLike.id DESC).
- `totalCount`는 현재 활성 항목 수다. 삭제된 항목은 포함하지 않는다.

---

## 3. Scenarios

### Scenario A. 프로필 조회
Given: 로그인한 사용자가 마이페이지에 접근한다.
When: `GET /members/me`
Then: 닉네임, 학번(공개 설정 시), 학번 공개 여부를 반환한다.

### Scenario B. 닉네임 변경
Given: 로그인한 사용자가 새 닉네임을 입력한다.
When: `PATCH /members/me` `{ "nickname": "새닉네임" }`
Then:
- 현재 닉네임과 동일하면 변경 없이 `200` 반환.
- 다른 사람이 사용 중인 닉네임이면 `409 DUPLICATE_NICKNAME`.
- 그 외 정상 변경.

### Scenario C. 내가 쓴 글 목록
Given: 로그인한 사용자가 마이페이지에서 "내가 쓴 글 전체보기"를 클릭한다.
When: `GET /members/me/posts?size=10`
Then: 내가 작성한 활성 게시글을 최신순으로 반환한다. `totalCount`와 커서를 포함한다.

### Scenario D. 내가 쓴 댓글 목록
Given: 로그인한 사용자가 "내가 쓴 댓글 전체보기"를 클릭한다.
When: `GET /members/me/comments?size=10`
Then: 내가 작성한 활성 댓글을 최신순으로 반환한다. 댓글이 달린 게시글 제목도 포함한다.

### Scenario E. 좋아요한 글 목록
Given: 로그인한 사용자가 "좋아요 전체보기"를 클릭한다.
When: `GET /members/me/likes?size=10`
Then: 내가 좋아요한 활성 게시글을 좋아요 시점 최신순으로 반환한다.

### Scenario F. 삭제된 게시글이 포함된 경우
Given: 내가 댓글을 달았던 게시글이 삭제됐다.
When: `GET /members/me/comments`
Then: 삭제된 게시글의 댓글은 목록에 포함되지 않는다.

---

## 4. Functional Requirements

- FR-1: `GET /members/me/posts`는 `cursor`(선택)와 `size`(기본값 10)를 받는다.
- FR-2: `GET /members/me/comments`는 댓글이 속한 게시글의 제목을 함께 반환한다.
- FR-3: `GET /members/me/likes`는 내가 좋아요한 게시글을 `PostLike.id DESC` 순서로 반환한다.
- FR-4: 세 API 모두 응답에 `totalCount`, `hasNext`, `nextCursor`, `items`를 포함한다.
- FR-5: 소프트 삭제된 게시글·댓글은 응답에 포함하지 않는다.
- FR-6: 미인증 상태에서 세 API 호출 시 `401 UNAUTHORIZED`.
- FR-7: 닉네임은 1~15자, 중복 불가.

---

## 5. Behavioral Rules

### 5.1 커서 페이지네이션 공통 규칙
- `cursor`가 없으면 첫 페이지 (가장 최신 항목부터).
- `cursor`가 있으면 해당 ID보다 작은 항목을 반환한다 (`id < cursor`).
- `size + 1`개를 조회해서 `hasNext` 여부를 판단한다.
- `nextCursor`는 마지막 항목의 `id`다. `hasNext`가 false면 `null`.

### 5.2 totalCount 처리
- `totalCount`는 현재 시점의 COUNT 쿼리로 반환한다.
- 커서 페이지네이션과 별도로 1회 실행한다.

### 5.3 댓글 목록에서 게시글 제목 포함
- `Comment JOIN FETCH post` 로 한 번에 조회한다.
- 게시글이 삭제된 댓글은 `post.deletedAt IS NULL` 조건으로 제외한다.

---

## 6. External Contracts

### API 요약

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| `GET` | `/members/me` | 필수 | 프로필 조회 |
| `PATCH` | `/members/me` | 필수 | 닉네임 변경 |
| `PATCH` | `/members/me/student-number-visibility` | 필수 | 학번 공개 설정 |
| `GET` | `/members/me/posts` | 필수 | 내가 쓴 글 목록 |
| `GET` | `/members/me/comments` | 필수 | 내가 쓴 댓글 목록 |
| `GET` | `/members/me/likes` | 필수 | 좋아요한 글 목록 |

### 응답 예시 (공통 페이지 구조)

```json
{
  "totalCount": 32,
  "hasNext": true,
  "nextCursor": 15,
  "items": [...]
}
```

### MyPostResponse
```json
{
  "postId": 42,
  "title": "이게 어떻게 할까요?",
  "boardType": "QNA",
  "createdAt": "2026-02-14T10:30:00"
}
```

### MyCommentResponse
```json
{
  "commentId": 101,
  "content": "노트북 팝니다",
  "postId": 38,
  "postTitle": "노트북 팝니다",
  "createdAt": "2026-02-10T15:20:00"
}
```

### MyLikeResponse
```json
{
  "postId": 20,
  "title": "후배들을 위한 꿀팁",
  "boardType": "FREE",
  "createdAt": "2026-01-05T09:00:00"
}
```

---

## 7. Non-Functional Constraints

- 댓글 목록 조회 시 `JOIN FETCH post`로 N+1 방지.
- `totalCount`와 페이지 데이터를 별도 쿼리로 분리해 정확도 보장.
