package kr.ac.knu.comit.comment.controller.api;

import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.comment.dto.AdminCommentPageResponse;
import kr.ac.knu.comit.comment.dto.AdminVisibilityRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
@RequestMapping("/admin/comments")
public interface AdminCommentControllerApi {

    @ApiDoc(
            summary = "댓글 목록 조회 (관리자)",
            description = "관리자가 댓글 목록을 조회합니다. 숨김 댓글을 포함하며, postId로 필터링할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "게시글 ID 필터입니다. 생략하면 전체를 조회합니다."),
                    @FieldDesc(name = "comments", value = "댓글 요약 목록입니다."),
                    @FieldDesc(name = "page", value = "현재 페이지 번호입니다. 0부터 시작합니다."),
                    @FieldDesc(name = "size", value = "페이지 크기입니다."),
                    @FieldDesc(name = "totalElements", value = "전체 댓글 수입니다."),
                    @FieldDesc(name = "totalPages", value = "전체 페이지 수입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "comments": [
                                  {
                                    "id": 1,
                                    "postId": 10,
                                    "parentCommentId": null,
                                    "content": "댓글 내용",
                                    "authorNickname": "author-1",
                                    "likeCount": 3,
                                    "hiddenByAdmin": false,
                                    "createdAt": "2026-03-28T10:00:00"
                                  }
                                ],
                                "page": 0,
                                "size": 20,
                                "totalElements": 1,
                                "totalPages": 1
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<AdminCommentPageResponse>> getComments(
            @RequestParam(required = false) Long postId,
            Pageable pageable,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "댓글 숨김/복원 (관리자)",
            description = "관리자가 댓글을 숨기거나 복원합니다.",
            descriptions = {
                    @FieldDesc(name = "commentId", value = "숨김/복원할 댓글 ID입니다."),
                    @FieldDesc(name = "hidden", value = "true이면 숨김, false이면 복원합니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "COMMENT_NOT_FOUND", when = "존재하지 않는 댓글 ID로 요청할 때")
            },
            example = @Example(
                    request = """
                            {
                              "hidden": true
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/{commentId}/visibility")
    ResponseEntity<ApiResponse<Void>> updateVisibility(
            @PathVariable Long commentId,
            @RequestBody AdminVisibilityRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "댓글 삭제 (관리자)",
            description = "관리자가 댓글을 소프트 삭제합니다.",
            descriptions = {
                    @FieldDesc(name = "commentId", value = "삭제할 댓글 ID입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "COMMENT_NOT_FOUND", when = "존재하지 않는 댓글 ID로 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @DeleteMapping("/{commentId}")
    ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberPrincipal principal
    );
}
