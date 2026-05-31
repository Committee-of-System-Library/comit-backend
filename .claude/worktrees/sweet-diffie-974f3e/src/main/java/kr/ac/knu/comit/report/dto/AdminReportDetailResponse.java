package kr.ac.knu.comit.report.dto;

import java.time.LocalDateTime;
import kr.ac.knu.comit.report.domain.Report;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;

public record AdminReportDetailResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        String message,
        String reporterNickname,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String reviewedByNickname
) {

    public static AdminReportDetailResponse from(Report report) {
        return new AdminReportDetailResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getMessage(),
                report.getReporter().getDisplayNickname(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getReviewedAt(),
                report.getReviewedBy() != null ? report.getReviewedBy().getDisplayNickname() : null
        );
    }
}
