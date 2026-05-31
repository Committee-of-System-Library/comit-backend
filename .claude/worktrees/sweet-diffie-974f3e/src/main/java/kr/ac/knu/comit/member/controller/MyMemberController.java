package kr.ac.knu.comit.member.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.member.controller.api.MyMemberControllerApi;
import kr.ac.knu.comit.member.dto.MyActivitySummaryResponse;
import kr.ac.knu.comit.member.dto.MyCommentCursorPageResponse;
import kr.ac.knu.comit.member.dto.MyLikedPostCursorPageResponse;
import kr.ac.knu.comit.member.service.MemberActivityService;
import kr.ac.knu.comit.post.dto.PostCursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyMemberController implements MyMemberControllerApi {

    private final MemberActivityService memberActivityService;

    @Override
    public ResponseEntity<ApiResponse<PostCursorPageResponse>> getMyPosts(
            Long cursor, int size, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                memberActivityService.getMyPosts(principal.memberId(), cursor, size)));
    }

    @Override
    public ResponseEntity<ApiResponse<MyActivitySummaryResponse>> getMyActivity(
            MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                memberActivityService.getActivitySummary(principal.memberId())));
    }

    @Override
    public ResponseEntity<ApiResponse<MyCommentCursorPageResponse>> getMyComments(
            Long cursor, int size, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                memberActivityService.getMyComments(principal.memberId(), cursor, size)));
    }

    @Override
    public ResponseEntity<ApiResponse<MyLikedPostCursorPageResponse>> getMyLikes(
            Long cursor, int size, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                memberActivityService.getMyLikes(principal.memberId(), cursor, size)));
    }
}
