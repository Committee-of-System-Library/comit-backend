CREATE TABLE official_notice
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    wr_id        VARCHAR(20),
    title        VARCHAR(300) NOT NULL,
    content      TEXT         NOT NULL,
    author       VARCHAR(100),
    original_url VARCHAR(500),
    posted_at    DATETIME(6),
    summary      TEXT,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6),
    deleted_at   DATETIME(6),

    UNIQUE KEY uk_official_notice_wr_id (wr_id),
    INDEX idx_official_notice_posted_at (posted_at DESC),
    INDEX idx_official_notice_deleted_at (deleted_at)
);
