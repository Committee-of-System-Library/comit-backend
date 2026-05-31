package kr.ac.knu.comit.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.fixture.CommentFixture;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.PostFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.ReportErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.service.PostService;
import kr.ac.knu.comit.report.domain.Report;
import kr.ac.knu.comit.report.domain.ReportRepository;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private PostService postService;

    @Mock
    private CommentQueryService commentQueryService;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("활성 게시글을 신고하면 접수 상태의 신고를 저장한다")
    void savesReceivedReportForPost() {
        // given
        // 활성 게시글과 신고자, 저장 결과를 준비한다.
        Post post = PostFixture.post(10L);
        Member reporter = MemberFixture.member(1L, "reporter");
        given(postService.getActivePostOrThrow(10L)).willReturn(post);
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L))
                .willReturn(false);
        given(memberService.findMemberOrThrow(1L)).willReturn(reporter);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });

        // when
        // 게시글 신고를 접수한다.
        Long reportId = reportService.reportPost(10L, 1L, "  광고성 도배입니다  ");

        // then
        // trim된 메시지와 RECEIVED 상태로 저장되어야 한다.
        assertThat(reportId).isEqualTo(101L);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        then(reportRepository).should().save(reportCaptor.capture());
        Report savedReport = reportCaptor.getValue();
        assertThat(ReflectionTestUtils.getField(savedReport, "targetType")).isEqualTo(ReportTargetType.POST);
        assertThat(ReflectionTestUtils.getField(savedReport, "targetId")).isEqualTo(10L);
        assertThat(ReflectionTestUtils.getField(savedReport, "message")).isEqualTo("광고성 도배입니다");
        assertThat(ReflectionTestUtils.getField(savedReport, "status")).isEqualTo(ReportStatus.RECEIVED);
        assertThat(ReflectionTestUtils.getField(savedReport, "reporter")).isEqualTo(reporter);
    }

    @Test
    @DisplayName("활성 댓글을 신고하면 댓글 대상 신고를 저장한다")
    void savesReceivedReportForComment() {
        // given
        // 활성 댓글과 신고자를 준비한다.
        Post post = PostFixture.post(10L);
        Member reporter = MemberFixture.member(1L, "reporter");
        given(commentQueryService.getActiveCommentOrThrow(20L))
                .willReturn(CommentFixture.topLevelComment(20L, post, MemberFixture.member(2L, "writer"), "욕설 댓글", 0));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.COMMENT, 20L))
                .willReturn(false);
        given(memberService.findMemberOrThrow(1L)).willReturn(reporter);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 202L);
            return saved;
        });

        // when
        // 댓글 신고를 접수한다.
        Long reportId = reportService.reportComment(20L, 1L, "욕설이 포함되어 있습니다");

        // then
        // 댓글 대상 신고 ID가 반환되어야 한다.
        assertThat(reportId).isEqualTo(202L);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        then(reportRepository).should().save(reportCaptor.capture());
        assertThat(ReflectionTestUtils.getField(reportCaptor.getValue(), "targetType")).isEqualTo(ReportTargetType.COMMENT);
        assertThat(ReflectionTestUtils.getField(reportCaptor.getValue(), "targetId")).isEqualTo(20L);
    }

    @Test
    @DisplayName("같은 사용자가 같은 게시글을 다시 신고하면 CONFLICT를 반환한다")
    void throwsWhenDuplicateReportExists() {
        // given
        // 동일 대상에 대한 기존 신고가 이미 있는 상황을 준비한다.
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L))
                .willReturn(true);

        // when & then
        // 중복 신고는 REPORT_ALREADY_EXISTS 예외가 발생해야 한다.
        assertThatThrownBy(() -> reportService.reportPost(10L, 1L, "중복 신고"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReportErrorCode.REPORT_ALREADY_EXISTS);

        // then
        // 회원 조회나 저장은 수행되지 않아야 한다.
        then(memberService).shouldHaveNoInteractions();
        then(reportRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("공백 메시지 신고는 INVALID_REQUEST를 반환한다")
    void throwsWhenMessageIsBlankAfterTrim() {
        // given
        // 활성 게시글과 신고자를 준비한다.
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L))
                .willReturn(false);
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "reporter"));

        // when & then
        // trim 후 빈 문자열이면 INVALID_REQUEST가 발생해야 한다.
        assertThatThrownBy(() -> reportService.reportPost(10L, 1L, "   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);

        // then
        // 잘못된 메시지는 저장되지 않아야 한다.
        then(reportRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("DB 유니크 제약 충돌도 중복 신고 예외로 변환한다")
    void convertsDataIntegrityViolationToDuplicateReportError() {
        // given
        // 선행 중복 검사 통과 후 저장 단계에서 유니크 충돌이 나는 상황을 준비한다.
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L))
                .willReturn(false);
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "reporter"));
        given(reportRepository.save(any(Report.class))).willThrow(duplicateKeyViolation());

        // when & then
        // 저장 충돌도 REPORT_ALREADY_EXISTS로 노출되어야 한다.
        assertThatThrownBy(() -> reportService.reportPost(10L, 1L, "광고성 도배"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReportErrorCode.REPORT_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("유니크 키가 아닌 무결성 오류는 그대로 전파한다")
    void propagatesNonDuplicateIntegrityViolations() {
        // given
        // 중복 신고가 아닌 다른 DB 무결성 오류 상황을 준비한다.
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L))
                .willReturn(false);
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "reporter"));
        given(reportRepository.save(any(Report.class))).willThrow(nonDuplicateIntegrityViolation());

        // when & then
        // 다른 무결성 오류는 REPORT_ALREADY_EXISTS로 숨기지 않아야 한다.
        assertThatThrownBy(() -> reportService.reportPost(10L, 1L, "광고성 도배"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private DataIntegrityViolationException duplicateKeyViolation() {
        SQLException sqlException = new SQLException(
                "Duplicate entry '1-POST-10' for key 'uk_report_reporter_target'",
                "23000",
                1062
        );
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "duplicate key",
                sqlException,
                "insert into report ...",
                "uk_report_reporter_target"
        );
        return new DataIntegrityViolationException("duplicate", constraintViolationException);
    }

    private DataIntegrityViolationException nonDuplicateIntegrityViolation() {
        SQLException sqlException = new SQLException(
                "Cannot add or update a child row: a foreign key constraint fails",
                "23000",
                1452
        );
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "fk violation",
                sqlException,
                "insert into report ...",
                "fk_report_reporter"
        );
        return new DataIntegrityViolationException("fk", constraintViolationException);
    }
}
