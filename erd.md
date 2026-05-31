# Comit ERD

ERDCloud 임포트: 아래 SQL DDL 블록을 ERDCloud > "Import" > "SQL" 탭에 붙여넣기

---

## 테이블 목록

| 테이블 | 설명 |
|--------|------|
| `member` | 회원 (SSO 기반, 정지/탈퇴/역할 관리) |
| `post` | 게시글 (게시판 유형별, 좋아요/조회수 내장) |
| `post_tag` | 게시글 태그 (게시글 생명주기 종속) |
| `post_image` | 게시글 첨부 이미지 (최대 5개, 순서 보존) |
| `post_like` | 게시글 좋아요 (post_id + member_id UNIQUE) |
| `post_daily_visitor` | 게시글 일별 조회 중복 방지 (post+member+날짜 UNIQUE) |
| `comment` | 댓글 / 대댓글 (self-join, 1단계 대댓글만 허용) |
| `comment_like` | 댓글 좋아요 (comment_id + member_id UNIQUE) |
| `report` | 신고 (게시글/댓글 대상, 관리자 처리 이력 포함) |

---

## ERDCloud 임포트용 SQL DDL

```sql
CREATE TABLE member (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    sso_sub               VARCHAR(255) NOT NULL,
    nickname              VARCHAR(50)  NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    phone                 VARCHAR(255) NOT NULL,
    profile_image_url     VARCHAR(500),
    student_number        VARCHAR(20),
    major_track           VARCHAR(255),
    student_number_visible BOOLEAN     NOT NULL DEFAULT TRUE,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'   COMMENT 'ACTIVE | SUSPENDED | BANNED',
    comit_role            VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'  COMMENT 'ADMIN | STUDENT',
    suspended_until       DATETIME,
    created_at            DATETIME     NOT NULL,
    agreed_at             DATETIME     NOT NULL,
    deleted_at            DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_sso_sub  (sso_sub),
    UNIQUE KEY uk_member_nickname (nickname)
);

CREATE TABLE post (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    member_id      BIGINT       NOT NULL,
    board_type     VARCHAR(20)  NOT NULL COMMENT 'QNA | FREE | INFO | NOTICE | EVENT',
    title          VARCHAR(255) NOT NULL,
    content        TEXT         NOT NULL,
    like_count     INT          NOT NULL DEFAULT 0,
    view_count     INT          NOT NULL DEFAULT 0,
    hidden_by_admin BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME,
    deleted_at     DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE TABLE post_tag (
    id      BIGINT      NOT NULL AUTO_INCREMENT,
    post_id BIGINT      NOT NULL,
    name    VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE TABLE post_image (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    post_id    BIGINT       NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_image_post FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE TABLE post_like (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    post_id    BIGINT   NOT NULL,
    member_id  BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_like (post_id, member_id)
);

CREATE TABLE post_daily_visitor (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    post_id    BIGINT   NOT NULL,
    member_id  BIGINT   NOT NULL,
    viewed_on  DATE     NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_daily_visitor (post_id, member_id, viewed_on)
);

CREATE TABLE comment (
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    post_id           BIGINT   NOT NULL,
    member_id         BIGINT   NOT NULL,
    parent_comment_id BIGINT,
    content           TEXT     NOT NULL,
    like_count        INT      NOT NULL DEFAULT 0,
    hidden_by_admin   BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME,
    deleted_at        DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_post          FOREIGN KEY (post_id)           REFERENCES post    (id),
    CONSTRAINT fk_comment_member        FOREIGN KEY (member_id)         REFERENCES member  (id),
    CONSTRAINT fk_comment_parent        FOREIGN KEY (parent_comment_id) REFERENCES comment (id)
);

CREATE TABLE comment_like (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    comment_id BIGINT   NOT NULL,
    member_id  BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_like (comment_id, member_id)
);

CREATE TABLE report (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    reporter_id  BIGINT       NOT NULL,
    target_type  VARCHAR(20)  NOT NULL COMMENT 'POST | COMMENT',
    target_id    BIGINT       NOT NULL,
    message      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL COMMENT 'RECEIVED | REVIEWED | DISMISSED | ACTIONED',
    created_at   DATETIME     NOT NULL,
    deleted_at   DATETIME,
    reviewed_at  DATETIME,
    reviewed_by  BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_report_reporter    FOREIGN KEY (reporter_id) REFERENCES member (id),
    CONSTRAINT fk_report_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES member (id)
);
```

---

## 관계 요약

```
member ──< post              (1:N  작성자)
member ──< comment           (1:N  작성자)
member ──< report.reporter   (1:N  신고자)
member ──< report.reviewed_by (1:N 검토자)

post ──< post_tag            (1:N  cascade all)
post ──< post_image          (1:N  cascade all, max 5)
post ──< post_like           (1:N  UNIQUE post_id+member_id)
post ──< post_daily_visitor  (1:N  UNIQUE post_id+member_id+viewed_on)
post ──< comment             (1:N)

comment ──< comment (self)   (1:N  parent_comment_id, 1단계만 허용)
comment ──< comment_like     (1:N  UNIQUE comment_id+member_id)

report.target_id → post.id or comment.id  (polymorphic, FK 없음)
```

---

## 열거형 값

| 열거형 | 값 |
|--------|-----|
| `member.status` | `ACTIVE` / `SUSPENDED` / `BANNED` |
| `member.comit_role` | `ADMIN` / `STUDENT` |
| `post.board_type` | `QNA` / `FREE` / `INFO` / `NOTICE` / `EVENT` |
| `report.target_type` | `POST` / `COMMENT` |
| `report.status` | `RECEIVED` / `REVIEWED` / `DISMISSED` / `ACTIONED` |
