package kr.ac.knu.comit.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.sql.SQLException;
import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.fixture.CommentFixture;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.PostFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.ReportErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.service.PostService;
import kr.ac.knu.comit.report.domain.FakeReportRepository;
import kr.ac.knu.comit.report.domain.Report;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService")
class ReportServiceTest {

    private FakeReportRepository reportRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private PostService postService;

    @Mock
    private CommentQueryService commentQueryService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportRepository = new FakeReportRepository();
        reportService = new ReportService(reportRepository, memberService, postService, commentQueryService);
    }

    @Test
    @DisplayName("활성 게시글을 신고하면 접수 상태의 신고를 저장한다")
    void savesReceivedReportForPost() {
        // given
        // 신고 대상 게시글과 신고자를 준비한다.
        Member reporter = MemberFixture.member(1L, "reporter");
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(memberService.findMemberOrThrow(1L)).willReturn(reporter);

        // when
        // 게시글 신고를 접수한다.
        Long reportId = reportService.reportPost(10L, 1L, "  광고성 도배입니다  ");

        // then
        // trim된 메시지와 RECEIVED 상태로 저장되어야 한다.
        assertThat(reportId).isNotNull();
        Report savedReport = reportRepository.findAll().get(0);
        assertThat(savedReport.getTargetType()).isEqualTo(ReportTargetType.POST);
        assertThat(savedReport.getTargetId()).isEqualTo(10L);
        assertThat(savedReport.getMessage()).isEqualTo("광고성 도배입니다");
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(savedReport.getReporter()).isEqualTo(reporter);
    }

    @Test
    @DisplayName("활성 댓글을 신고하면 댓글 대상 신고를 저장한다")
    void savesReceivedReportForComment() {
        // given
        // 신고 대상 댓글과 신고자를 준비한다.
        Member reporter = MemberFixture.member(1L, "reporter");
        given(commentQueryService.getActiveCommentOrThrow(20L))
                .willReturn(CommentFixture.topLevelComment(20L, PostFixture.post(10L), MemberFixture.member(2L, "writer"), "욕설 댓글", 0));
        given(memberService.findMemberOrThrow(1L)).willReturn(reporter);

        // when
        // 댓글 신고를 접수한다.
        Long reportId = reportService.reportComment(20L, 1L, "욕설이 포함되어 있습니다");

        // then
        // 댓글 대상 신고 ID가 반환되어야 한다.
        assertThat(reportId).isNotNull();
        Report savedReport = reportRepository.findAll().get(0);
        assertThat(savedReport.getTargetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(savedReport.getTargetId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("같은 사용자가 같은 게시글을 다시 신고하면 CONFLICT를 반환한다")
    void throwsWhenDuplicateReportExists() {
        // given
        // 동일 대상에 대한 기존 신고를 저장소에 미리 등록한다.
        reportRepository.save(Report.create(MemberFixture.member(1L, "reporter"), ReportTargetType.POST, 10L, "기존 신고"));
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));

        // when & then
        // 중복 신고는 REPORT_ALREADY_EXISTS 예외가 발생해야 한다.
        assertThatThrownBy(() -> reportService.reportPost(10L, 1L, "중복 신고"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReportErrorCode.REPORT_ALREADY_EXISTS);

        // then
        // 회원 조회나 새 신고 저장은 수행되지 않아야 한다.
        then(memberService).shouldHaveNoInteractions();
        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("공백 메시지 신고는 INVALID_REQUEST를 반환한다")
    void throwsWhenMessageIsBlankAfterTrim() {
        // given
        // 활성 게시글과 신고자를 준비한다.
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "reporter"));

        // when & then
        // trim 후 빈 문자열이면 INVALID_REQUEST가 발생해야 한다.
        assertThatThrownBy(() -> reportService.reportPost(10L, 1L, "   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);

        // then
        // 잘못된 메시지는 저장되지 않아야 한다.
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("DB 유니크 제약 충돌도 중복 신고 예외로 변환한다")
    void convertsDataIntegrityViolationToDuplicateReportError() {
        // given
        // 저장 단계에서 DB 유니크 제약 충돌이 발생하는 상황을 시뮬레이션한다.
        reportRepository.onNextSaveThrow(duplicateKeyViolation());
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "reporter"));

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
        // 중복 신고가 아닌 다른 DB 무결성 오류 상황을 시뮬레이션한다.
        reportRepository.onNextSaveThrow(nonDuplicateIntegrityViolation());
        given(postService.getActivePostOrThrow(10L)).willReturn(PostFixture.post(10L));
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "reporter"));

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
