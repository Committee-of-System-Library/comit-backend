package kr.ac.knu.comit.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.ReportErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Report")
class ReportTest {

    @Nested
    @DisplayName("review")
    class Review {

        @Test
        @DisplayName("RECEIVED 상태에서 review 호출 시 상태를 전이하고 reviewer, reviewedAt을 설정한다")
        void transitionsFromReceivedToNewStatus() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = Report.create(reporter, ReportTargetType.POST, 10L, "신고 사유");

            // when
            report.review(reviewer, ReportStatus.REVIEWED);

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.REVIEWED);
            assertThat(report.getReviewedBy()).isEqualTo(reviewer);
            assertThat(report.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("RECEIVED 상태에서 DISMISSED로 전이할 수 있다")
        void transitionsFromReceivedToDismissed() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = Report.create(reporter, ReportTargetType.POST, 10L, "신고 사유");

            // when
            report.review(reviewer, ReportStatus.DISMISSED);

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        }

        @Test
        @DisplayName("RECEIVED 상태에서 ACTIONED로 전이할 수 있다")
        void transitionsFromReceivedToActioned() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = Report.create(reporter, ReportTargetType.POST, 10L, "신고 사유");

            // when
            report.review(reviewer, ReportStatus.ACTIONED);

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.ACTIONED);
        }

        @Test
        @DisplayName("RECEIVED를 다시 요청하면 INVALID_REQUEST가 발생한다")
        void throwsWhenRequestedStatusIsReceived() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = Report.create(reporter, ReportTargetType.POST, 10L, "신고 사유");

            // when & then
            assertThatThrownBy(() -> report.review(reviewer, ReportStatus.RECEIVED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("REVIEWED 상태에서 review 호출 시 예외가 발생한다")
        void throwsWhenAlreadyReviewed() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = Report.create(reporter, ReportTargetType.POST, 10L, "신고 사유");
            ReflectionTestUtils.setField(report, "status", ReportStatus.REVIEWED);

            // when & then
            assertThatThrownBy(() -> report.review(reviewer, ReportStatus.ACTIONED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReportErrorCode.REPORT_ALREADY_REVIEWED);
        }

        @Test
        @DisplayName("DISMISSED 상태에서 review 호출 시 예외가 발생한다")
        void throwsWhenAlreadyDismissed() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = Report.create(reporter, ReportTargetType.POST, 10L, "신고 사유");
            ReflectionTestUtils.setField(report, "status", ReportStatus.DISMISSED);

            // when & then
            assertThatThrownBy(() -> report.review(reviewer, ReportStatus.REVIEWED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReportErrorCode.REPORT_ALREADY_REVIEWED);
        }

        @Test
        @DisplayName("ACTIONED 상태에서 review 호출 시 예외가 발생한다")
        void throwsWhenAlreadyActioned() {
            // given
            Member reporter = MemberFixture.member(1L, "reporter");
            Member reviewer = MemberFixture.member(2L, "admin");
            Report report = Report.create(reporter, ReportTargetType.POST, 10L, "신고 사유");
            ReflectionTestUtils.setField(report, "status", ReportStatus.ACTIONED);

            // when & then
            assertThatThrownBy(() -> report.review(reviewer, ReportStatus.REVIEWED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReportErrorCode.REPORT_ALREADY_REVIEWED);
        }
    }
}
