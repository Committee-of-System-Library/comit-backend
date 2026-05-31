CREATE TABLE comment
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id       BIGINT      NOT NULL,
    member_id     BIGINT      NOT NULL,
    content       TEXT        NOT NULL,
    helpful_count INT         NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6),
    deleted_at    DATETIME(6),

    -- helpful_count DESC 정렬 지원 (도움이요 많은 댓글 상단)
    INDEX idx_comment_post_helpful (post_id, helpful_count DESC, id ASC),
    INDEX idx_comment_member_id (member_id),

    CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)   REFERENCES post (id),
    CONSTRAINT fk_comment_member FOREIGN KEY (member_id) REFERENCES member (id)
);
