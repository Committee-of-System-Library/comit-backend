package kr.ac.knu.comit.member.controller.api;

import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.member.dto.MyActivitySummaryResponse;
import kr.ac.knu.comit.member.dto.MyCommentCursorPageResponse;
import kr.ac.knu.comit.member.dto.MyLikedPostCursorPageResponse;
import kr.ac.knu.comit.post.dto.PostCursorPageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
public interface MyMemberControllerApi {

    @ApiDoc(
            summary = "내가 쓴 게시글 목록 조회",
            description = "현재 로그인한 사용자가 작성한 게시글을 cursor 기반으로 최신순 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "cursor", value = "이전 응답의 nextCursorId. 첫 페이지는 생략합니다."),
                    @FieldDesc(name = "size", value = "조회할 게시글 수입니다. 기본값은 20이고 최대 20입니다."),
                    @FieldDesc(name = "posts", value = "내가 작성한 게시글 요약 목록입니다."),
                    @FieldDesc(name = "nextCursorId", value = "다음 페이지 조회에 사용할 마지막 게시글 ID입니다. 마지막 페이지면 null입니다."),
                    @FieldDesc(name = "hasNext", value = "다음 페이지 존재 여부입니다.")
            },
            errors = {
                    @ApiError(code = "INVALID_REQUEST", when = "size가 1 이상이 아닐 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "posts": [
                                  {
                                    "id": 101,
                                    "boardType": "QNA",
                                    "title": "JPA fetch join 질문",
                                    "authorNickname": "backend-dev",
                                    "likeCount": 3,
                                    "commentCount": 2,
                                    "tags": ["spring", "jpa"],
                                    "imageUrls": [],
                                    "createdAt": "2026-03-24T10:00:00"
                                  }
                                ],
                                "nextCursorId": 100,
                                "hasNext": true
                              }
                            }
                            """
            )
    )
    @GetMapping("members/me/posts")
    ResponseEntity<ApiResponse<PostCursorPageResponse>> getMyPosts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "내 활동 요약 조회",
            description = "내가 쓴 글/댓글/좋아요 수와 최근 3개 미리보기를 한 번에 반환합니다.",
            descriptions = {
                    @FieldDesc(name = "postCount", value = "내가 작성한 게시글 수입니다."),
                    @FieldDesc(name = "commentCount", value = "내가 작성한 댓글 수입니다."),
                    @FieldDesc(name = "likeCount", value = "내가 좋아요한 활성 게시글 수입니다."),
                    @FieldDesc(name = "recentPosts", value = "최근 게시글 최대 3개입니다. 각 항목은 id, title, createdAt을 포함합니다."),
                    @FieldDesc(name = "recentComments", value = "최근 댓글 최대 3개입니다. 각 항목은 id, content, postId, postTitle, boardType, createdAt을 포함합니다."),
                    @FieldDesc(name = "recentLikes", value = "최근 좋아요한 게시글 최대 3개입니다. 각 항목은 postId, postTitle, boardType, likedAt을 포함합니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "postCount": 15,
                                "commentCount": 42,
                                "likeCount": 8,
                                "recentPosts": [
                                  { "id": 101, "title": "JPA fetch join 질문", "createdAt": "2026-04-01T10:00:00" }
                                ],
                                "recentComments": [
                                  { "id": 201, "content": "감사합니다!", "postId": 50, "postTitle": "QueryDSL 정리", "boardType": "QNA", "createdAt": "2026-04-02T11:00:00" }
                                ],
                                "recentLikes": [
                                  { "postId": 80, "postTitle": "Redis 캐싱 전략", "boardType": "QNA", "likedAt": "2026-04-03T10:00:00" }
                                ]
                              }
                            }
                            """
            )
    )
    @GetMapping("members/me/activity")
    ResponseEntity<ApiResponse<MyActivitySummaryResponse>> getMyActivity(
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "내가 쓴 댓글 목록 조회",
            description = "현재 로그인한 사용자가 작성한 댓글을 cursor 기반으로 최신순 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "cursor", value = "이전 응답의 nextCursorId. 첫 페이지는 생략합니다."),
                    @FieldDesc(name = "size", value = "조회할 댓글 수입니다. 기본값은 20이고 최대 20입니다."),
                    @FieldDesc(name = "comments", value = "댓글 목록입니다. 각 항목은 id, content, postId, postTitle, boardType, createdAt을 포함합니다."),
                    @FieldDesc(name = "nextCursorId", value = "다음 페이지 조회에 사용할 마지막 댓글 ID입니다. 마지막 페이지면 null입니다."),
                    @FieldDesc(name = "hasNext", value = "다음 페이지 존재 여부입니다.")
            },
            errors = {
                    @ApiError(code = "INVALID_REQUEST", when = "size가 1 이상이 아닐 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "comments": [
                                  {
                                    "id": 201,
                                    "content": "감사합니다!",
                                    "postId": 50,
                                    "postTitle": "QueryDSL 정리",
                                    "boardType": "QNA",
                                    "createdAt": "2026-04-02T11:00:00"
                                  }
                                ],
                                "nextCursorId": 195,
                                "hasNext": true
                              }
                            }
                            """
            )
    )
    @GetMapping("members/me/comments")
    ResponseEntity<ApiResponse<MyCommentCursorPageResponse>> getMyComments(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "내가 좋아요한 게시글 목록 조회",
            description = "현재 로그인한 사용자가 좋아요한 게시글을 cursor 기반으로 최신순 조회합니다. 삭제/숨김 처리된 게시글은 목록에서 제외됩니다.",
            descriptions = {
                    @FieldDesc(name = "cursor", value = "이전 응답의 nextCursorId. 첫 페이지는 생략합니다."),
                    @FieldDesc(name = "size", value = "조회할 게시글 수입니다. 기본값은 20이고 최대 20입니다."),
                    @FieldDesc(name = "posts", value = "좋아요한 게시글 목록입니다. 각 항목은 postId, postTitle, boardType, likedAt을 포함합니다."),
                    @FieldDesc(name = "nextCursorId", value = "다음 페이지 조회에 사용할 마지막 좋아요 내부 ID입니다. 마지막 페이지면 null입니다."),
                    @FieldDesc(name = "hasNext", value = "다음 페이지 존재 여부입니다.")
            },
            errors = {
                    @ApiError(code = "INVALID_REQUEST", when = "size가 1 이상이 아닐 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "posts": [
                                  {
                                    "postId": 80,
                                    "postTitle": "Redis 캐싱 전략",
                                    "boardType": "QNA",
                                    "likedAt": "2026-04-03T10:00:00"
                                  }
                                ],
                                "nextCursorId": 120,
                                "hasNext": true
                              }
                            }
                            """
            )
    )
    @GetMapping("members/me/likes")
    ResponseEntity<ApiResponse<MyLikedPostCursorPageResponse>> getMyLikes(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticatedMember MemberPrincipal principal
    );
}
