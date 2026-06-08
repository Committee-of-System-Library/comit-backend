UPDATE student_council_fee scf
JOIN member m ON scf.member_id = m.id
SET scf.student_number = m.student_number
WHERE scf.student_number IS NULL;

ALTER TABLE student_council_fee
    DROP PRIMARY KEY;

ALTER TABLE student_council_fee
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

ALTER TABLE student_council_fee
    MODIFY COLUMN student_number VARCHAR(20) NOT NULL,
    DROP COLUMN member_id;
