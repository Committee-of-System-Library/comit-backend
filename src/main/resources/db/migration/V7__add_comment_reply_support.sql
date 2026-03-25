ALTER TABLE comment
    ADD COLUMN parent_comment_id BIGINT NULL AFTER member_id,
    ADD INDEX idx_comment_parent_id (parent_comment_id),
    ADD CONSTRAINT fk_comment_parent
        FOREIGN KEY (parent_comment_id) REFERENCES comment (id);
