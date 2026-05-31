ALTER TABLE member
    ADD COLUMN name VARCHAR(255) NULL AFTER nickname,
    ADD COLUMN phone VARCHAR(255) NULL AFTER name,
    ADD COLUMN major_track VARCHAR(255) NULL AFTER student_number,
    ADD COLUMN agreed_at DATETIME(6) NULL AFTER created_at;

UPDATE member
SET name = COALESCE(name, 'unknown'),
    phone = COALESCE(phone, 'unknown'),
    agreed_at = COALESCE(agreed_at, created_at, CURRENT_TIMESTAMP(6));

ALTER TABLE member
    MODIFY COLUMN name VARCHAR(255) NOT NULL,
    MODIFY COLUMN phone VARCHAR(255) NOT NULL,
    MODIFY COLUMN agreed_at DATETIME(6) NOT NULL;
