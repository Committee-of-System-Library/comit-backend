package kr.ac.knu.comit.comment.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CreateCommentRequest;
import kr.ac.knu.comit.comment.dto.LikeToggleResponse;
import kr.ac.knu.comit.comment.dto.UpdateCommentRequest;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.report.dto.CreateReportRequest;
import kr.ac.knu.comit.report.dto.CreateReportResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@ApiContract
public interface CommentControllerApi {

    @ApiDoc(
            summary = "댓글 목록 조회",
            description = "게시글에 달린 댓글 목록을 조회합니다. 좋아요 수 내림차순, 동일하면 댓글 ID 오름차순으로 정렬됩니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "댓글을 조회할 게시글 ID"),
                    @FieldDesc(name = "comments", value = "최상위 댓글 목록입니다. 각 항목은 댓글 ID, 본문, 작성자 닉네임, 좋아요 수, 내 반응 여부, 내 작성 여부, 생성/수정 시각과 대댓글 목록을 포함합니다."),
                    @FieldDesc(name = "id", value = "댓글 ID입니다."),
                    @FieldDesc(name = "content", value = "댓글 본문입니다."),
                    @FieldDesc(name = "authorNickname", value = "댓글 작성자의 닉네임입니다."),
                    @FieldDesc(name = "likeCount", value = "현재 댓글의 좋아요 수입니다."),
                    @FieldDesc(name = "likedByMe", value = "현재 로그인한 사용자가 좋아요를 눌렀는지 여부입니다."),
                    @FieldDesc(name = "mine", value = "현재 로그인한 사용자가 작성한 댓글인지 여부입니다."),
                    @FieldDesc(name = "replies", value = "현재 댓글에 달린 대댓글 목록입니다. 대댓글은 등록 순으로 반환됩니다."),
                    @FieldDesc(name = "createdAt", value = "댓글 생성 시각입니다. 응답 포맷은 yyyy-MM-dd'T'HH:mm:ss 입니다."),
                    @FieldDesc(name = "updatedAt", value = "마지막 수정 시각입니다. 수정 이력이 없으면 null입니다. 응답 포맷은 yyyy-MM-dd'T'HH:mm:ss 입니다.")
            },
            errors = {
                    @ApiError(code = "POST_NOT_FOUND", when = "댓글을 조회할 게시글이 없거나 삭제된 상태일 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "comments": [
                                  {
                                    "id": 201,
                                    "content": "entity graph도 같이 비교해보면 좋습니다.",
                                    "authorNickname": "orm-master",
                                    "likeCount": 4,
                                    "likedByMe": true,
                                    "mine": false,
                                    "createdAt": "2026-03-24T11:00:00",
                                    "updatedAt": null,
                                    "replies": [
                                      {
                                        "id": 202,
                                        "content": "저도 같은 방식 추천합니다.",
                                        "authorNickname": "backend-dev",
                                        "likeCount": 1,
                                        "likedByMe": false,
                                        "mine": true,
                                        "createdAt": "2026-03-24T11:05:00",
                                        "updatedAt": null
                                      }
                                    ]
                                  }
                                ]
                              }
                            }
                            """
            )
    )
    @GetMapping("/posts/{postId}/comments")
    ResponseEntity<ApiResponse<CommentListResponse>> getComments(
            @PathVariable Long postId,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "댓글 작성",
            description = "게시글에 새 댓글 또는 대댓글을 작성합니다. parentCommentId가 없으면 일반 댓글, 있으면 해당 댓글의 대댓글로 생성됩니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "댓글을 작성할 게시글 ID"),
                    @FieldDesc(name = "parentCommentId", value = "선택 항목입니다. 대댓글 대상 최상위 댓글 ID입니다. 대댓글의 대댓글은 허용하지 않습니다."),
                    @FieldDesc(name = "content", value = "댓글 본문입니다. 비어 있을 수 없습니다.")
            },
            errors = {
                    @ApiError(code = "POST_NOT_FOUND", when = "댓글을 작성할 게시글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "COMMENT_NOT_FOUND", when = "parentCommentId로 지정한 댓글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "INVALID_PARENT_COMMENT", when = "다른 게시글의 댓글이거나 이미 대댓글인 댓글에 답글을 달려고 할 때"),
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "인증된 사용자의 로컬 회원 정보가 존재하지 않을 때")
            },
            example = @Example(
                    request = """
                            {
                              "parentCommentId": 201,
                              "content": "entity graph도 같이 비교해보면 좋습니다."
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": 201
                            }
                            """
            )
    )
    @PostMapping("/posts/{postId}/comments")
    ResponseEntity<ApiResponse<Long>> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CreateCommentRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "댓글 수정",
            description = "내가 작성한 댓글의 내용을 수정합니다.",
            descriptions = {
                    @FieldDesc(name = "commentId", value = "수정할 댓글 ID"),
                    @FieldDesc(name = "content", value = "수정할 댓글 본문입니다. 비어 있을 수 없습니다.")
            },
            errors = {
                    @ApiError(code = "COMMENT_NOT_FOUND", when = "수정 대상 댓글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "FORBIDDEN", when = "작성자가 아닌 사용자가 댓글을 수정하려고 할 때")
            },
            example = @Example(
                    request = """
                            {
                              "content": "entity graph와 fetch join 차이도 비교해보면 좋겠습니다."
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/comments/{commentId}")
    ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "댓글 삭제",
            description = "내가 작성한 댓글을 삭제 상태로 변경합니다. 최상위 댓글을 삭제하면 해당 대댓글도 함께 삭제됩니다.",
            descriptions = {
                    @FieldDesc(name = "commentId", value = "삭제할 댓글 ID")
            },
            errors = {
                    @ApiError(code = "COMMENT_NOT_FOUND", when = "삭제 대상 댓글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "FORBIDDEN", when = "작성자가 아닌 사용자가 댓글을 삭제하려고 할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @DeleteMapping("/comments/{commentId}")
    ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "댓글 신고",
            description = "활성 댓글을 신고합니다. 신고 사유는 자유 입력 메시지이며 최대 500자까지 허용합니다.",
            descriptions = {
                    @FieldDesc(name = "commentId", value = "신고할 댓글 ID입니다."),
                    @FieldDesc(name = "message", value = "신고 사유 메시지입니다. 공백만 입력할 수 없고 최대 500자까지 허용합니다."),
                    @FieldDesc(name = "reportId", value = "생성된 신고 ID입니다.")
            },
            errors = {
                    @ApiError(code = "UNAUTHORIZED", when = "인증되지 않은 사용자가 신고하려고 할 때"),
                    @ApiError(code = "COMMENT_NOT_FOUND", when = "신고 대상 댓글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "신고 메시지가 비어 있거나 500자를 초과할 때"),
                    @ApiError(code = "REPORT_ALREADY_EXISTS", when = "같은 사용자가 같은 댓글을 이미 신고했을 때")
            },
            example = @Example(
                    request = """
                            {
                              "message": "욕설이 포함되어 있습니다"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "reportId": 302
                              }
                            }
                            """
            )
    )
    @PostMapping("/comments/{commentId}/reports")
    ResponseEntity<ApiResponse<CreateReportResponse>> reportComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CreateReportRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "댓글 좋아요 토글",
            description = "댓글의 좋아요 상태를 토글합니다. 응답은 토글 후 내 반응 상태만 반환하며 likeCount는 목록 재조회 시 확인합니다.",
            descriptions = {
                    @FieldDesc(name = "commentId", value = "좋아요를 토글할 댓글 ID"),
                    @FieldDesc(name = "liked", value = "true면 좋아요가 추가되고 false면 취소됩니다.")
            },
            errors = {
                    @ApiError(code = "COMMENT_NOT_FOUND", when = "좋아요를 누를 댓글이 없거나 삭제된 상태일 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "liked": true
                              }
                            }
                            """
            )
    )
    @PostMapping("/comments/{commentId}/like")
    ResponseEntity<ApiResponse<LikeToggleResponse>> toggleLike(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberPrincipal principal
    );
}
