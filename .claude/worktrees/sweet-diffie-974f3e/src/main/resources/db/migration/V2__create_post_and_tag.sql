CREATE TABLE post
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    board_type  VARCHAR(20)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    like_count  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6),
    deleted_at  DATETIME(6),

    -- cursor 페이지네이션: board_type + id DESC 복합 인덱스
    INDEX idx_post_board_cursor (board_type, id DESC),
    INDEX idx_post_member_id (member_id),

    CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE TABLE post_tag
(
    id      BIGINT      AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT      NOT NULL,
    name    VARCHAR(20) NOT NULL,

    INDEX idx_post_tag_post_id (post_id),
    INDEX idx_post_tag_name (name),

    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post (id)
);
