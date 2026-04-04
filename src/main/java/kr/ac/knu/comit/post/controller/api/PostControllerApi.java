package kr.ac.knu.comit.post.controller.api;

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
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ApiContract
@RequestMapping("/posts")
public interface PostControllerApi {

    @ApiDoc(
            summary = "게시글 목록 조회",
            description = "게시판별 게시글 목록을 cursor 기반으로 최신순 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "boardType", value = "조회할 게시판 유형입니다. QNA 또는 FREE를 사용합니다."),
                    @FieldDesc(name = "cursor", value = "이전 응답의 nextCursorId. 첫 페이지는 생략합니다."),
                    @FieldDesc(name = "size", value = "조회할 게시글 수입니다. 기본값은 20이고 최대 20입니다."),
                    @FieldDesc(name = "posts", value = "게시글 요약 목록입니다. 최신 게시글부터 최대 size개를 반환하며 각 항목은 게시글 ID, 제목, 본문 미리보기, 작성자 닉네임, 좋아요 수, 댓글 수, 태그, 생성 시각을 포함합니다."),
                    @FieldDesc(name = "id", value = "게시글 ID입니다."),
                    @FieldDesc(name = "title", value = "게시글 제목입니다."),
                    @FieldDesc(name = "contentPreview", value = "게시글 본문 미리보기입니다. 줄바꿈과 중복 공백을 정리한 뒤 최대 80자까지만 제공하며, 초과 시 ...이 붙습니다."),
                    @FieldDesc(name = "authorNickname", value = "게시글 작성자의 닉네임입니다."),
                    @FieldDesc(name = "likeCount", value = "현재 게시글의 좋아요 수입니다."),
                    @FieldDesc(name = "commentCount", value = "삭제되지 않은 댓글 수입니다."),
                    @FieldDesc(name = "tags", value = "게시글에 연결된 태그 목록입니다."),
                    @FieldDesc(name = "imageUrls", value = "이미지 url이고, url 조회시 바로 이미지를 받을 수 있습니다."),
                    @FieldDesc(name = "createdAt", value = "게시글 생성 시각입니다. 응답 포맷은 yyyy-MM-dd'T'HH:mm:ss 입니다."),
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
                                    "contentPreview": "join fetch와 entity graph 차이가 궁금합니다.",
                                    "authorNickname": "backend-dev",
                                    "likeCount": 3,
                                    "commentCount": 2,
                                    "tags": [
                                      "spring",
                                      "jpa"
                                    ],
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
    @GetMapping
    ResponseEntity<ApiResponse<PostCursorPageResponse>> getPosts(
            @RequestParam BoardType boardType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "인기글 상위 5개 조회",
            description = "최근 7일 기준 좋아요, 댓글, unique 방문자 반응을 가중치로 계산한 인기글 상위 5개를 조회합니다. 점수는 외부에 노출하지 않고 rank만 반환합니다.",
            descriptions = {
                    @FieldDesc(name = "posts", value = "인기글 목록입니다. 점수 0 초과인 게시글만 포함되며 최대 5개까지 반환합니다."),
                    @FieldDesc(name = "rank", value = "인기글 순위입니다. 1부터 시작합니다."),
                    @FieldDesc(name = "id", value = "게시글 ID입니다."),
                    @FieldDesc(name = "boardType", value = "게시글이 속한 게시판 유형입니다."),
                    @FieldDesc(name = "title", value = "게시글 제목입니다."),
                    @FieldDesc(name = "authorNickname", value = "게시글 작성자의 닉네임입니다."),
                    @FieldDesc(name = "likeCount", value = "현재 게시글의 좋아요 수입니다."),
                    @FieldDesc(name = "commentCount", value = "삭제되지 않은 댓글 수입니다."),
                    @FieldDesc(name = "tags", value = "게시글에 연결된 태그 목록입니다."),
                    @FieldDesc(name = "createdAt", value = "게시글 생성 시각입니다. 응답 포맷은 yyyy-MM-dd'T'HH:mm:ss 입니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "posts": [
                                  {
                                    "rank": 1,
                                    "id": 101,
                                    "boardType": "QNA",
                                    "title": "JPA fetch join 질문",
                                    "authorNickname": "backend-dev",
                                    "likeCount": 3,
                                    "commentCount": 4,
                                    "tags": [
                                      "spring",
                                      "jpa"
                                    ],
                                    "createdAt": "2026-03-24T10:00:00"
                                  }
                                ]
                              }
                            }
                            """
            )
    )
    @GetMapping("/hot")
    ResponseEntity<ApiResponse<HotPostListResponse>> getHotPosts(
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 상세 조회",
            description = "게시글 하나의 상세 정보를 조회합니다. 조회가 성공하면 응답의 viewCount에 반영된 누적 조회수가 1 증가합니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "조회할 게시글 ID입니다."),
                    @FieldDesc(name = "id", value = "게시글 ID입니다."),
                    @FieldDesc(name = "boardType", value = "게시글이 속한 게시판 유형입니다."),
                    @FieldDesc(name = "title", value = "게시글 제목입니다."),
                    @FieldDesc(name = "content", value = "게시글 본문입니다."),
                    @FieldDesc(name = "authorNickname", value = "게시글 작성자의 닉네임입니다."),
                    @FieldDesc(name = "likeCount", value = "현재 게시글의 좋아요 수입니다."),
                    @FieldDesc(name = "viewCount", value = "상세 조회 성공이 반영된 최신 누적 조회수입니다."),
                    @FieldDesc(name = "likedByMe", value = "현재 로그인한 사용자의 좋아요 여부입니다."),
                    @FieldDesc(name = "tags", value = "게시글에 연결된 태그 목록입니다."),
                    @FieldDesc(name = "imageUrls", value = "이미지 url이고, url 조회시 바로 이미지를 받을 수 있습니다."),
                    @FieldDesc(name = "createdAt", value = "게시글 생성 시각입니다. 응답 포맷은 yyyy-MM-dd'T'HH:mm:ss 입니다."),
                    @FieldDesc(name = "updatedAt", value = "마지막 수정 시각입니다. 수정 이력이 없으면 null입니다. 응답 포맷은 yyyy-MM-dd'T'HH:mm:ss 입니다.")
            },
            errors = {
                    @ApiError(code = "POST_NOT_FOUND", when = "조회 대상 게시글이 없거나 삭제된 상태일 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "id": 101,
                                "boardType": "QNA",
                                "title": "JPA fetch join 질문",
                                "content": "join fetch와 entity graph 차이가 궁금합니다.",
                                "authorNickname": "backend-dev",
                                "likeCount": 3,
                                "viewCount": 128,
                                "likedByMe": true,
                                "tags": [
                                  "spring",
                                  "jpa"
                                ],
                                "createdAt": "2026-03-24T10:00:00",
                                "updatedAt": "2026-03-24T10:30:00"
                              }
                            }
                            """
            )
    )
    @GetMapping("/{postId}")
    ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            @PathVariable Long postId,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 작성",
            description = "새 게시글을 작성합니다. 제목은 최대 30자, 본문은 최대 500자까지 허용합니다.",
            descriptions = {
                    @FieldDesc(name = "boardType", value = "게시글을 작성할 게시판 유형입니다."),
                    @FieldDesc(name = "title", value = "게시글 제목입니다. 최대 30자입니다."),
                    @FieldDesc(name = "content", value = "게시글 본문입니다. 최대 500자입니다."),
                    @FieldDesc(name = "tags", value = "선택 항목입니다. 최대 5개, 각 20자 이하입니다."),
                    @FieldDesc(name = "imageUrls", value = "이미지 url이고, url 조회시 바로 이미지를 받을 수 있습니다. 최대 5개까지 첨부할 수 있습니다.")
            },
            errors = {
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "인증된 사용자의 로컬 회원 정보가 존재하지 않을 때"),
                    @ApiError(code = "INVALID_TAG", when = "태그 길이가 도메인 규칙을 벗어날 때")
            },
            example = @Example(
                    request = """
                            {
                              "boardType": "QNA",
                              "title": "JPA fetch join 질문",
                              "content": "join fetch와 entity graph 차이가 궁금합니다.",
                              "tags": [
                                "spring",
                                "jpa"
                              ]
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": 101
                            }
                            """
            )
    )
    @PostMapping
    ResponseEntity<ApiResponse<Long>> createPost(
            @RequestBody @Valid CreatePostRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 수정",
            description = "기존 게시글의 제목, 본문, 태그를 수정합니다. 제목은 최대 30자, 본문은 최대 500자까지 허용합니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "수정할 게시글 ID입니다."),
                    @FieldDesc(name = "title", value = "수정할 게시글 제목입니다. 최대 30자입니다."),
                    @FieldDesc(name = "content", value = "수정할 게시글 본문입니다. 최대 500자입니다."),
                    @FieldDesc(name = "tags", value = "수정할 태그 목록입니다. null이면 빈 목록으로 처리합니다.")
            },
            errors = {
                    @ApiError(code = "POST_NOT_FOUND", when = "수정 대상 게시글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "FORBIDDEN", when = "작성자가 아닌 사용자가 수정하려고 할 때"),
                    @ApiError(code = "INVALID_TAG", when = "태그 길이가 도메인 규칙을 벗어날 때")
            },
            example = @Example(
                    request = """
                            {
                              "title": "JPA fetch join 정리",
                              "content": "join fetch와 entity graph 차이를 정리했습니다.",
                              "tags": [
                                "spring",
                                "orm"
                              ]
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/{postId}")
    ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 삭제",
            description = "게시글을 삭제 상태로 변경합니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "삭제할 게시글 ID입니다.")
            },
            errors = {
                    @ApiError(code = "POST_NOT_FOUND", when = "삭제 대상 게시글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "FORBIDDEN", when = "작성자 또는 관리자 권한 없이 삭제하려고 할 때")
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

    @ApiDoc(
            summary = "게시글 신고",
            description = "활성 게시글을 신고합니다. 신고 사유는 자유 입력 메시지이며 최대 500자까지 허용합니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "신고할 게시글 ID입니다."),
                    @FieldDesc(name = "message", value = "신고 사유 메시지입니다. 공백만 입력할 수 없고 최대 500자까지 허용합니다."),
                    @FieldDesc(name = "reportId", value = "생성된 신고 ID입니다.")
            },
            errors = {
                    @ApiError(code = "UNAUTHORIZED", when = "인증되지 않은 사용자가 신고하려고 할 때"),
                    @ApiError(code = "POST_NOT_FOUND", when = "신고 대상 게시글이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "신고 메시지가 비어 있거나 500자를 초과할 때"),
                    @ApiError(code = "REPORT_ALREADY_EXISTS", when = "같은 사용자가 같은 게시글을 이미 신고했을 때")
            },
            example = @Example(
                    request = """
                            {
                              "message": "광고성 도배입니다"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "reportId": 301
                              }
                            }
                            """
            )
    )
    @PostMapping("/{postId}/reports")
    ResponseEntity<ApiResponse<CreateReportResponse>> reportPost(
            @PathVariable Long postId,
            @RequestBody @Valid CreateReportRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "게시글 좋아요 토글",
            description = "현재 로그인한 사용자의 게시글 좋아요 상태를 토글합니다.",
            descriptions = {
                    @FieldDesc(name = "postId", value = "좋아요를 토글할 게시글 ID입니다."),
                    @FieldDesc(name = "liked", value = "true면 좋아요가 추가되고 false면 좋아요가 취소됩니다.")
            },
            errors = {
                    @ApiError(code = "POST_NOT_FOUND", when = "좋아요를 누를 게시글이 없거나 삭제된 상태일 때")
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
    @PostMapping("/{postId}/like")
    ResponseEntity<ApiResponse<LikeToggleResponse>> toggleLike(
            @PathVariable Long postId,
            @AuthenticatedMember MemberPrincipal principal
    );
}
