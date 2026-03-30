package kr.ac.knu.comit.report.dto;

import jakarta.validation.constraints.NotNull;
import kr.ac.knu.comit.report.domain.ReportStatus;

public record ReviewReportRequest(
        @NotNull
        ReportStatus status
) {
}
