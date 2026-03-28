package kr.ac.knu.comit.post.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.post.controller.api.PostControllerApi;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.dto.*;
import kr.ac.knu.comit.post.service.PostService;
import kr.ac.knu.comit.report.dto.CreateReportRequest;
import kr.ac.knu.comit.report.dto.CreateReportResponse;
import kr.ac.knu.comit.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostController implements PostControllerApi {

    private final PostService postService;
    private final ReportService reportService;

    @Override
    public ResponseEntity<ApiResponse<PostCursorPageResponse>> getPosts(
            BoardType boardType, Long cursor, int size, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.getPosts(boardType, cursor, size)));
    }

    @Override
    public ResponseEntity<ApiResponse<HotPostListResponse>> getHotPosts(
            MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(postService.getHotPosts()));
    }

    @Override
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            Long postId, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.getPost(postId, principal.memberId())));
    }

    @Override
    public ResponseEntity<ApiResponse<Long>> createPost(
            CreatePostRequest request, MemberPrincipal principal) {
        Long postId = postService.createPost(principal.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(postId));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updatePost(
            Long postId, UpdatePostRequest request, MemberPrincipal principal) {
        postService.updatePost(principal.memberId(), postId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deletePost(
            Long postId, MemberPrincipal principal) {
        if (principal.isAdmin()) {
            postService.forceDeletePost(postId);
        } else {
            postService.deletePost(principal.memberId(), postId);
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<CreateReportResponse>> reportPost(
            Long postId, CreateReportRequest request, MemberPrincipal principal) {
        Long reportId = reportService.reportPost(postId, principal.memberId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(CreateReportResponse.from(reportId)));
    }

    @Override
    public ResponseEntity<ApiResponse<LikeToggleResponse>> toggleLike(
            Long postId, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.toggleLike(principal.memberId(), postId)));
    }
}
