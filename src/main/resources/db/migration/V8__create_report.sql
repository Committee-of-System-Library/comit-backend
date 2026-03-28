CREATE TABLE report
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT      NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT      NOT NULL,
    message     TEXT        NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6),
    reviewed_by BIGINT,

    UNIQUE KEY uk_report_reporter_target (reporter_id, target_type, target_id),
    INDEX idx_report_target (target_type, target_id),
    INDEX idx_report_status_created_at (status, created_at DESC),

    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES member (id),
    CONSTRAINT fk_report_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES member (id)
);
