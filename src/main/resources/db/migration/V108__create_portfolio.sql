CREATE TABLE portfolio
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id          BIGINT       NOT NULL,
    file_url           VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size          BIGINT       NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NULL,
    deleted_at         DATETIME     NULL,
    INDEX idx_portfolio_member_id (member_id)
);