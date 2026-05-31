package kr.ac.knu.comit.report.dto;

public record CreateReportResponse(Long reportId) {

    public static CreateReportResponse from(Long reportId) {
        return new CreateReportResponse(reportId);
    }
}
