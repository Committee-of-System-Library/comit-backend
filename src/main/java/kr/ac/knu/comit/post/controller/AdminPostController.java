package kr.ac.knu.comit.post.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.post.controller.api.AdminPostControllerApi;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.dto.AdminPostPageResponse;
import kr.ac.knu.comit.post.dto.AdminVisibilityRequest;
import kr.ac.knu.comit.post.service.AdminPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminPostController implements AdminPostControllerApi {

    private final AdminPostService adminPostService;

    @Override
    public ResponseEntity<ApiResponse<AdminPostPageResponse>> getPosts(
            BoardType boardType, Pageable pageable, MemberPrincipal principal) {
        validateAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(
                adminPostService.getPosts(boardType, pageable)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updateVisibility(
            Long postId, AdminVisibilityRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        if (request.hidden()) {
            adminPostService.hidePost(postId);
        } else {
            adminPostService.restorePost(postId);
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deletePost(
            Long postId, MemberPrincipal principal) {
        validateAdmin(principal);
        adminPostService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateAdmin(MemberPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
