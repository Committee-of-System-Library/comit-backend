package kr.ac.knu.comit.comment.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.comment.controller.api.AdminCommentControllerApi;
import kr.ac.knu.comit.comment.dto.AdminCommentPageResponse;
import kr.ac.knu.comit.comment.dto.AdminVisibilityRequest;
import kr.ac.knu.comit.comment.service.AdminCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminCommentController implements AdminCommentControllerApi {

    private final AdminCommentService adminCommentService;

    @Override
    public ResponseEntity<ApiResponse<AdminCommentPageResponse>> getComments(
            Long postId, Pageable pageable, MemberPrincipal principal) {
        validateAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(
                adminCommentService.getComments(postId, pageable)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updateVisibility(
            Long commentId, AdminVisibilityRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        if (request.hidden()) {
            adminCommentService.hideComment(commentId);
        } else {
            adminCommentService.restoreComment(commentId);
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            Long commentId, MemberPrincipal principal) {
        validateAdmin(principal);
        adminCommentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateAdmin(MemberPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
