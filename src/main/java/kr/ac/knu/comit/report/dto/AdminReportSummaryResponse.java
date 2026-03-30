package kr.ac.knu.comit.report.dto;

import java.time.LocalDateTime;
import kr.ac.knu.comit.report.domain.Report;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;

public record AdminReportSummaryResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        String message,
        String reporterNickname,
        ReportStatus status,
        LocalDateTime createdAt
) {

    public static AdminReportSummaryResponse from(Report report) {
        return new AdminReportSummaryResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getMessage(),
                report.getReporter().getNickname(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
