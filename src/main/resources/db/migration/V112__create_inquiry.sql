CREATE TABLE inquiry
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    title      VARCHAR(30)  NOT NULL,
    content    TEXT         NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_inquiry_member FOREIGN KEY (member_id) REFERENCES member (id)
);
