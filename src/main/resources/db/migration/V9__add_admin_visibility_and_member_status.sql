ALTER TABLE member
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER student_number_visible,
    ADD COLUMN suspended_until DATETIME(6) NULL AFTER status;

ALTER TABLE post
    ADD COLUMN hidden_by_admin TINYINT(1) NOT NULL DEFAULT 0 AFTER view_count;

ALTER TABLE comment
    ADD COLUMN hidden_by_admin TINYINT(1) NOT NULL DEFAULT 0 AFTER helpful_count;
