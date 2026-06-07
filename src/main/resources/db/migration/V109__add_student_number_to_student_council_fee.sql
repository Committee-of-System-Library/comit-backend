ALTER TABLE student_council_fee
    ADD COLUMN student_number VARCHAR(20) NULL AFTER member_id,
    ADD UNIQUE KEY uk_student_council_fee_student_number (student_number);
