package kr.ac.knu.comit.post.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.dto.AdminCreatePostRequest;
import kr.ac.knu.comit.post.dto.AdminCreatePostResponse;
import kr.ac.knu.comit.post.dto.AdminPostPageResponse;
import kr.ac.knu.comit.post.dto.AdminVisibilityRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
@RequestMapping("/admin/posts")
public interface AdminPostControllerApi {

    @ApiDoc(
            summary = "공지/이벤트/정보 게시글 작성 (관리자)",
            description = "관리자가 NOTICE, EVENT, INFO 게시판에 게시글을 작성합니다. QNA, FREE 게시판에는 작성할 수 없습니다.",
            descriptions = {
                    @FieldDesc(name = "boardType", value = "게시판 유형입니다. NOTICE, EVENT, INFO 중 하나만 허용됩니다."),
                    @FieldDesc(name = "title", value = "게시글 제목입니다."),
                    @FieldDesc(name = "content", value = "게시글 내용입니다."),
                    @FieldDesc(name = "tags", value = "태그 목록입니다. 생략 가능합니다."),
                    @FieldDesc(name = "imageUrls", value = "첨부 이미지 URL 목록입니다. 생략 가능합니다."),
                    @FieldDesc(name = "postId", value = "생성된 게시글 ID입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "FORBIDDEN_BOARD_TYPE", when = "NOTICE, EVENT, INFO 외 boardType으로 요청할 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "요청 본문이 검증 규칙을 만족하지 않을 때")
            },
            example = @Example(
                    request = """
                            {
                              "boardType": "NOTICE",
                              "title": "2026년 1학기 정기 모집 공고",
                              "content": "모집 내용입니다.",
                              "tags": ["모집", "공지"],
                              "imageUrls": []
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": { "postId": 42 }
                            }
                            """
            )
    )
    @PostMapping
    ResponseEntity<ApiResponse<AdminCreatePostResponse>> createPost(
            @Valid @RequestBody AdminCreatePostRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 목록 조회 (관리자)",
            description = "관리자가 게시글 목록을 조회합니다. 숨김 게시글을 포함하며, boardType으로 필터링할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "boardType", value = "게시판 유형 필터입니다. 생략하면 전체를 조회합니다."),
                    @FieldDesc(name = "posts", value = "게시글 요약 목록입니다."),
                    @FieldDesc(name = "page", value = "현재 페이지 번호입니다. 0부터 시작합니다."),
                    @FieldDesc(name = "size", value = "페이지 크기입니다."),
                    @FieldDesc(name = "totalElements", value = "전체 게시글 수입니다."),
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
                                "posts": [
                                  {
                                    "id": 1,
                                    "boardType": "FREE",
                                    "title": "게시글 제목",
                                    "authorNickname": "author-1",
                                    "likeCount": 5,
                                    "viewCount": 100,
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
    ResponseEntity<ApiResponse<AdminPostPageResponse>> getPosts(
            @RequestParam(required = false) BoardType boardType,
            Pageable pageable,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 숨김/복원 (관리자)",
            description = "관리자가 게시글을 숨기거나 복원합니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "숨김/복원할 게시글 ID입니다."),
                    @FieldDesc(name = "hidden", value = "true이면 숨김, false이면 복원합니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "POST_NOT_FOUND", when = "존재하지 않는 게시글 ID로 요청할 때")
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
    @PatchMapping("/{postId}/visibility")
    ResponseEntity<ApiResponse<Void>> updateVisibility(
            @PathVariable Long postId,
            @RequestBody AdminVisibilityRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 삭제 (관리자)",
            description = "관리자가 게시글을 소프트 삭제합니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "삭제할 게시글 ID입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "POST_NOT_FOUND", when = "존재하지 않는 게시글 ID로 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @DeleteMapping("/{postId}")
    ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticatedMember MemberPrincipal principal
    );
}
