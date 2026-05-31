package kr.ac.knu.comit.report.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.ReportErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.report.domain.Report;
import kr.ac.knu.comit.report.domain.ReportRepository;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import kr.ac.knu.comit.report.dto.AdminReportDetailResponse;
import kr.ac.knu.comit.report.dto.AdminReportPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final MemberService memberService;

    public AdminReportPageResponse getReports(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        Page<Report> reportPage = reportRepository.findAllActiveByFilters(status, targetType, pageable);
        return AdminReportPageResponse.from(reportPage);
    }

    public AdminReportDetailResponse getReport(Long reportId) {
        Report report = findReportOrThrow(reportId);
        return AdminReportDetailResponse.from(report);
    }

    @Transactional
    public void reviewReport(Long reportId, Long reviewerId, ReportStatus newStatus) {
        Report report = findReportOrThrow(reportId);
        Member reviewer = memberService.findMemberOrThrow(reviewerId);
        report.review(reviewer, newStatus);
    }

    private Report findReportOrThrow(Long reportId) {
        return reportRepository.findByIdAndDeletedAtIsNull(reportId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));
    }
}
