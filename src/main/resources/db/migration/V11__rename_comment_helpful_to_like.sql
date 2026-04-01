-- Rename comment reaction persistence names without changing data.
RENAME TABLE comment_helpful TO comment_like;

ALTER TABLE comment CHANGE helpful_count like_count INT NOT NULL DEFAULT 0;
