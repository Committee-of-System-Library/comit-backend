package kr.ac.knu.comit.report.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.report.controller.api.AdminReportControllerApi;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import kr.ac.knu.comit.report.dto.AdminReportDetailResponse;
import kr.ac.knu.comit.report.dto.AdminReportPageResponse;
import kr.ac.knu.comit.report.dto.ReviewReportRequest;
import kr.ac.knu.comit.report.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminReportController implements AdminReportControllerApi {

    private final AdminReportService adminReportService;

    @Override
    public ResponseEntity<ApiResponse<AdminReportPageResponse>> getReports(
            ReportStatus status, ReportTargetType targetType, Pageable pageable, MemberPrincipal principal) {
        validateAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(
                adminReportService.getReports(status, targetType, pageable)));
    }

    @Override
    public ResponseEntity<ApiResponse<AdminReportDetailResponse>> getReport(
            Long reportId, MemberPrincipal principal) {
        validateAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(
                adminReportService.getReport(reportId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> reviewReport(
            Long reportId, ReviewReportRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        adminReportService.reviewReport(reportId, principal.memberId(), request.status());
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateAdmin(MemberPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
