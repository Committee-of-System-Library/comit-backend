ALTER TABLE member
    ADD COLUMN profile_image_url VARCHAR(500) NULL AFTER phone;

CREATE TABLE post_image
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT       NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_post_image_post FOREIGN KEY (post_id) REFERENCES post (id),
    INDEX idx_post_image_post_id (post_id)
);
