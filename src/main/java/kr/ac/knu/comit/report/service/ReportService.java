package kr.ac.knu.comit.report.service;

import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.ReportErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.service.PostService;
import kr.ac.knu.comit.report.domain.Report;
import kr.ac.knu.comit.report.domain.ReportRepository;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberService memberService;
    private final PostService postService;
    private final CommentQueryService commentQueryService;

    @Transactional
    public Long reportPost(Long postId, Long memberId, String message) {
        postService.getActivePostOrThrow(postId);
        return createReport(memberId, ReportTargetType.POST, postId, message);
    }

    @Transactional
    public Long reportComment(Long commentId, Long memberId, String message) {
        commentQueryService.getActiveCommentOrThrow(commentId);
        return createReport(memberId, ReportTargetType.COMMENT, commentId, message);
    }

    private Long createReport(Long memberId, ReportTargetType targetType, Long targetId, String message) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(memberId, targetType, targetId)) {
            throw new BusinessException(ReportErrorCode.REPORT_ALREADY_EXISTS);
        }

        Member reporter = memberService.findMemberOrThrow(memberId);
        Report report = Report.create(reporter, targetType, targetId, message);

        try {
            return reportRepository.save(report).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ReportErrorCode.REPORT_ALREADY_EXISTS);
        }
    }
}
