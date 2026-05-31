CREATE TABLE member
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sso_sub                 VARCHAR(255) NOT NULL,
    nickname                VARCHAR(50)  NOT NULL,
    student_number_visible  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at              DATETIME(6)  NOT NULL,
    deleted_at              DATETIME(6),

    UNIQUE KEY uk_member_sso_sub (sso_sub),
    UNIQUE KEY uk_member_nickname (nickname)
);
