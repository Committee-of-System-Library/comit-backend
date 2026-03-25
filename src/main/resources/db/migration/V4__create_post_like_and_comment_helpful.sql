-- 동시성 핵심: (post_id, member_id) UNIQUE → DB가 중복 좋아요를 원천 차단
-- INSERT IGNORE 패턴으로 race-condition 없는 토글 구현
CREATE TABLE post_like
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,

    UNIQUE KEY uk_post_like (post_id, member_id),

    CONSTRAINT fk_post_like_post   FOREIGN KEY (post_id)   REFERENCES post (id),
    CONSTRAINT fk_post_like_member FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE TABLE comment_helpful
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,

    UNIQUE KEY uk_comment_helpful (comment_id, member_id),

    CONSTRAINT fk_comment_helpful_comment FOREIGN KEY (comment_id) REFERENCES comment (id),
    CONSTRAINT fk_comment_helpful_member  FOREIGN KEY (member_id)  REFERENCES member (id)
);
