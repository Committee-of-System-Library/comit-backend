package kr.ac.knu.comit.report.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import kr.ac.knu.comit.report.domain.Report;

public record AdminReportPageResponse(
        List<AdminReportSummaryResponse> reports,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static AdminReportPageResponse from(Page<Report> reportPage) {
        List<AdminReportSummaryResponse> reports = reportPage.getContent().stream()
                .map(AdminReportSummaryResponse::from)
                .toList();
        return new AdminReportPageResponse(
                reports,
                reportPage.getNumber(),
                reportPage.getSize(),
                reportPage.getTotalElements(),
                reportPage.getTotalPages()
        );
    }
}
