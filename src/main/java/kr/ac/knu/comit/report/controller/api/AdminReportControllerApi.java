package kr.ac.knu.comit.report.controller.api;

import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import kr.ac.knu.comit.report.dto.AdminReportDetailResponse;
import kr.ac.knu.comit.report.dto.AdminReportPageResponse;
import kr.ac.knu.comit.report.dto.ReviewReportRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
@RequestMapping("/admin/reports")
public interface AdminReportControllerApi {

    @ApiDoc(
            summary = "신고 목록 조회",
            description = "관리자가 접수된 신고 목록을 필터링하여 조회합니다. status와 targetType은 선택 필터입니다.",
            descriptions = {
                    @FieldDesc(name = "status", value = "신고 상태 필터입니다. RECEIVED, REVIEWED, DISMISSED, ACTIONED 중 하나를 사용합니다. 생략하면 전체를 조회합니다."),
                    @FieldDesc(name = "targetType", value = "신고 대상 유형 필터입니다. POST 또는 COMMENT를 사용합니다. 생략하면 전체를 조회합니다."),
                    @FieldDesc(name = "reports", value = "신고 요약 목록입니다."),
                    @FieldDesc(name = "page", value = "현재 페이지 번호입니다. 0부터 시작합니다."),
                    @FieldDesc(name = "size", value = "페이지 크기입니다."),
                    @FieldDesc(name = "totalElements", value = "전체 신고 수입니다."),
                    @FieldDesc(name = "totalPages", value = "전체 페이지 수입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "reports": [
                                  {
                                    "id": 1,
                                    "targetType": "POST",
                                    "targetId": 10,
                                    "message": "광고성 도배입니다",
                                    "reporterNickname": "reporter-1",
                                    "status": "RECEIVED",
                                    "createdAt": "2026-03-28T10:00:00"
                                  }
                                ],
                                "page": 0,
                                "size": 20,
                                "totalElements": 1,
                                "totalPages": 1
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<AdminReportPageResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            Pageable pageable,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "신고 상세 조회",
            description = "관리자가 신고 상세 정보를 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "reportId", value = "조회할 신고 ID입니다."),
                    @FieldDesc(name = "id", value = "신고 ID입니다."),
                    @FieldDesc(name = "targetType", value = "신고 대상 유형입니다."),
                    @FieldDesc(name = "targetId", value = "신고 대상 ID입니다."),
                    @FieldDesc(name = "message", value = "신고 사유 메시지입니다."),
                    @FieldDesc(name = "reporterNickname", value = "신고자 닉네임입니다."),
                    @FieldDesc(name = "status", value = "신고 상태입니다."),
                    @FieldDesc(name = "createdAt", value = "신고 생성 시각입니다."),
                    @FieldDesc(name = "reviewedAt", value = "처리 시각입니다. 미처리 상태이면 null입니다."),
                    @FieldDesc(name = "reviewedByNickname", value = "처리한 관리자 닉네임입니다. 미처리 상태이면 null입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "REPORT_NOT_FOUND", when = "존재하지 않는 신고 ID로 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "id": 1,
                                "targetType": "POST",
                                "targetId": 10,
                                "message": "광고성 도배입니다",
                                "reporterNickname": "reporter-1",
                                "status": "REVIEWED",
                                "createdAt": "2026-03-28T10:00:00",
                                "reviewedAt": "2026-03-28T11:00:00",
                                "reviewedByNickname": "admin-1"
                              }
                            }
                            """
            )
    )
    @GetMapping("/{reportId}")
    ResponseEntity<ApiResponse<AdminReportDetailResponse>> getReport(
            @PathVariable Long reportId,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "신고 상태 변경",
            description = "관리자가 접수된 신고의 상태를 변경합니다. RECEIVED 상태의 신고만 변경할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "reportId", value = "상태를 변경할 신고 ID입니다."),
                    @FieldDesc(name = "status", value = "변경할 상태입니다. REVIEWED, DISMISSED, ACTIONED 중 하나를 사용합니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "REPORT_NOT_FOUND", when = "존재하지 않는 신고 ID로 요청할 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "변경 상태가 REVIEWED, DISMISSED, ACTIONED 중 하나가 아닐 때"),
                    @ApiError(code = "REPORT_ALREADY_REVIEWED", when = "이미 처리된 신고의 상태를 변경하려 할 때")
            },
            example = @Example(
                    request = """
                            {
                              "status": "REVIEWED"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/{reportId}/status")
    ResponseEntity<ApiResponse<Void>> reviewReport(
            @PathVariable Long reportId,
            @RequestBody @Valid ReviewReportRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );
}
