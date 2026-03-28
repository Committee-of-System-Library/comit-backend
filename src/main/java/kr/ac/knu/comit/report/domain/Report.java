package kr.ac.knu.comit.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.domain.Member;

@Entity
@Table(name = "report")
public class Report {

    private static final int MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Member reviewedBy;

    protected Report() {
    }

    public static Report create(
            Member reporter,
            ReportTargetType targetType,
            Long targetId,
            String message
    ) {
        validateReporter(reporter);
        validateTarget(targetType, targetId);
        Report report = new Report();
        report.reporter = reporter;
        report.targetType = targetType;
        report.targetId = targetId;
        report.message = normalizeMessage(message);
        report.status = ReportStatus.RECEIVED;
        report.createdAt = LocalDateTime.now();
        return report;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static String normalizeMessage(String message) {
        if (message == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        String normalized = message.strip();
        if (normalized.isEmpty() || normalized.length() > MESSAGE_MAX_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private static void validateReporter(Member reporter) {
        if (reporter == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateTarget(ReportTargetType targetType, Long targetId) {
        if (targetType == null || targetId == null || targetId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }
}
