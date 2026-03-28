package kr.ac.knu.comit.comment.controller;

import kr.ac.knu.comit.comment.controller.api.CommentControllerApi;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CreateCommentRequest;
import kr.ac.knu.comit.comment.dto.HelpfulToggleResponse;
import kr.ac.knu.comit.comment.dto.UpdateCommentRequest;
import kr.ac.knu.comit.comment.service.CommentService;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.report.dto.CreateReportRequest;
import kr.ac.knu.comit.report.dto.CreateReportResponse;
import kr.ac.knu.comit.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController implements CommentControllerApi {

    private final CommentService commentService;
    private final ReportService reportService;

    @Override
    public ResponseEntity<ApiResponse<CommentListResponse>> getComments(Long postId, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postId, principal.memberId())));
    }

    @Override
    public ResponseEntity<ApiResponse<Long>> createComment(
            Long postId,
            CreateCommentRequest request,
            MemberPrincipal principal
    ) {
        Long commentId = commentService.createComment(postId, principal.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(commentId));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updateComment(
            Long commentId,
            UpdateCommentRequest request,
            MemberPrincipal principal
    ) {
        commentService.updateComment(commentId, principal.memberId(), request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteComment(Long commentId, MemberPrincipal principal) {
        commentService.deleteComment(commentId, principal.memberId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<CreateReportResponse>> reportComment(
            Long commentId,
            CreateReportRequest request,
            MemberPrincipal principal
    ) {
        Long reportId = reportService.reportComment(commentId, principal.memberId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(CreateReportResponse.from(reportId)));
    }

    @Override
    public ResponseEntity<ApiResponse<HelpfulToggleResponse>> toggleHelpful(
            Long commentId,
            MemberPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.toggleHelpful(commentId, principal.memberId())
        ));
    }
}
