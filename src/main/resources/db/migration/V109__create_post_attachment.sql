CREATE TABLE post_attachment
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id            BIGINT       NOT NULL UNIQUE,
    file_type          VARCHAR(20)  NOT NULL,
    file_url           VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size          BIGINT       NOT NULL
);