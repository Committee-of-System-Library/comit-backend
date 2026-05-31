ALTER TABLE post
    ADD COLUMN view_count INT NOT NULL DEFAULT 0 AFTER like_count;

CREATE TABLE post_daily_visitor
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    viewed_on  DATE        NOT NULL,
    created_at DATETIME(6) NOT NULL,

    UNIQUE KEY uk_post_daily_visitor (post_id, member_id, viewed_on),
    INDEX idx_post_daily_visitor_post_day (post_id, viewed_on),

    CONSTRAINT fk_post_daily_visitor_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT fk_post_daily_visitor_member FOREIGN KEY (member_id) REFERENCES member (id)
);
