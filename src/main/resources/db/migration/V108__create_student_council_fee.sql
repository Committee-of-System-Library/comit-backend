CREATE TABLE student_council_fee (
    member_id BIGINT NOT NULL,
    is_paid   BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (member_id)
);
