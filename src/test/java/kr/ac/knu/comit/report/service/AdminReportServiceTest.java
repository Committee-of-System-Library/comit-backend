package kr.ac.knu.comit.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.ReportFixture;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReportService")
class AdminReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private AdminReportService adminReportService;

    @Nested
    @DisplayName("getReports")
    class GetReports {

        @Test
        @DisplayName("필터 없이 전체 신고 목록을 조회한다")
        void returnsAllReports() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Report report = ReportFixture.report(1L, reporter);
            Pageable pageable = PageRequest.of(0, 20);
            given(reportRepository.findAllActiveByFilters(null, null, pageable))
                    .willReturn(new PageImpl<>(List.of(report), pageable, 1));

            // when
            AdminReportPageResponse response = adminReportService.getReports(null, null, pageable);

            // then
            assertThat(response.reports()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("status 필터로 신고 목록을 조회한다")
        void returnsFilteredByStatus() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Report report = ReportFixture.report(1L, reporter);
            Pageable pageable = PageRequest.of(0, 20);
            given(reportRepository.findAllActiveByFilters(ReportStatus.RECEIVED, null, pageable))
                    .willReturn(new PageImpl<>(List.of(report), pageable, 1));

            // when
            AdminReportPageResponse response = adminReportService.getReports(ReportStatus.RECEIVED, null, pageable);

            // then
            assertThat(response.reports()).hasSize(1);
            then(reportRepository).should().findAllActiveByFilters(ReportStatus.RECEIVED, null, pageable);
        }

        @Test
        @DisplayName("targetType 필터로 신고 목록을 조회한다")
        void returnsFilteredByTargetType() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Report report = ReportFixture.report(1L, reporter);
            Pageable pageable = PageRequest.of(0, 20);
            given(reportRepository.findAllActiveByFilters(null, ReportTargetType.POST, pageable))
                    .willReturn(new PageImpl<>(List.of(report), pageable, 1));

            // when
            AdminReportPageResponse response = adminReportService.getReports(null, ReportTargetType.POST, pageable);

            // then
            assertThat(response.reports()).hasSize(1);
            then(reportRepository).should().findAllActiveByFilters(null, ReportTargetType.POST, pageable);
        }
    }

    @Nested
    @DisplayName("getReport")
    class GetReport {

        @Test
        @DisplayName("존재하는 신고의 상세 정보를 반환한다")
        void returnsReportDetail() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Report report = ReportFixture.report(1L, reporter);
            given(reportRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(report));

            // when
            AdminReportDetailResponse response = adminReportService.getReport(1L);

            // then
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.reporterNickname()).isEqualTo("reporter");
            assertThat(response.reviewedAt()).isNull();
            assertThat(response.reviewedByNickname()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 신고 ID로 조회하면 예외가 발생한다")
        void throwsWhenReportNotFound() {
            // given
            given(reportRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReportService.getReport(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReportErrorCode.REPORT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("reviewReport")
    class ReviewReport {

        @Test
        @DisplayName("RECEIVED 상태의 신고를 정상적으로 상태 변경한다")
        void reviewsReceivedReport() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = ReportFixture.report(1L, reporter);
            given(reportRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(report));
            given(memberService.findMemberOrThrow(2L)).willReturn(reviewer);

            // when
            adminReportService.reviewReport(1L, 2L, ReportStatus.REVIEWED);

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.REVIEWED);
            assertThat(report.getReviewedBy()).isEqualTo(reviewer);
            assertThat(report.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 처리된 신고를 변경하면 예외가 발생한다")
        void throwsWhenAlreadyReviewed() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = ReportFixture.reviewedReport(1L, reporter, ReportStatus.REVIEWED);
            given(reportRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(report));
            given(memberService.findMemberOrThrow(2L)).willReturn(reviewer);

            // when & then
            assertThatThrownBy(() -> adminReportService.reviewReport(1L, 2L, ReportStatus.ACTIONED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReportErrorCode.REPORT_ALREADY_REVIEWED);
        }

        @Test
        @DisplayName("RECEIVED로 상태 변경을 요청하면 예외가 발생한다")
        void throwsWhenRequestedStatusIsReceived() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = ReportFixture.report(1L, reporter);
            given(reportRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(report));
            given(memberService.findMemberOrThrow(2L)).willReturn(reviewer);

            // when & then
            assertThatThrownBy(() -> adminReportService.reviewReport(1L, 2L, ReportStatus.RECEIVED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(kr.ac.knu.comit.global.exception.CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("존재하지 않는 신고를 변경하면 예외가 발생한다")
        void throwsWhenReportNotFound() {
            // given
            given(reportRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReportService.reviewReport(999L, 2L, ReportStatus.REVIEWED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReportErrorCode.REPORT_NOT_FOUND);
        }
    }
}
