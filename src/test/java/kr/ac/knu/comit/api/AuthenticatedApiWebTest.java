package kr.ac.knu.comit.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knu.comit.comment.controller.AdminCommentController;
import kr.ac.knu.comit.comment.controller.CommentController;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CommentResponse;
import kr.ac.knu.comit.comment.dto.ReplyResponse;
import kr.ac.knu.comit.comment.service.AdminCommentService;
import kr.ac.knu.comit.comment.service.CommentService;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.global.auth.MemberArgumentResolver;
import kr.ac.knu.comit.global.auth.MemberAuthenticationFilter;
import kr.ac.knu.comit.global.config.WebMvcConfig;
import kr.ac.knu.comit.global.exception.GlobalExceptionHandler;
import kr.ac.knu.comit.main.controller.MainController;
import kr.ac.knu.comit.main.dto.MainPageResponse;
import kr.ac.knu.comit.main.service.MainService;
import kr.ac.knu.comit.member.controller.AdminMemberController;
import kr.ac.knu.comit.member.controller.MemberController;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.service.AdminMemberService;
import kr.ac.knu.comit.member.dto.MemberProfileResponse;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.controller.AdminPostController;
import kr.ac.knu.comit.post.controller.PostController;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.PostConstraints;
import kr.ac.knu.comit.post.service.AdminPostService;
import kr.ac.knu.comit.post.dto.HotPostListResponse;
import kr.ac.knu.comit.post.dto.HotPostResponse;
import kr.ac.knu.comit.post.dto.PostCursorPageResponse;
import kr.ac.knu.comit.post.dto.PostDetailResponse;
import kr.ac.knu.comit.post.dto.PostSummaryResponse;
import kr.ac.knu.comit.post.service.PostService;
import kr.ac.knu.comit.report.controller.AdminReportController;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import kr.ac.knu.comit.report.dto.AdminReportDetailResponse;
import kr.ac.knu.comit.report.dto.AdminReportPageResponse;
import kr.ac.knu.comit.report.dto.AdminReportSummaryResponse;
import kr.ac.knu.comit.report.service.AdminReportService;
import kr.ac.knu.comit.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@WebMvcTest({
        MemberController.class,
        PostController.class,
        CommentController.class,
        MainController.class,
        AdminMemberController.class,
        AdminPostController.class,
        AdminCommentController.class,
        AdminReportController.class
})
@Import({
        WebMvcConfig.class,
        MemberArgumentResolver.class,
        MemberAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        AdminMemberService.class
})
@ActiveProfiles("local")
class AuthenticatedApiWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private MainService mainService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private AdminReportService adminReportService;

    @MockitoBean
    private AdminPostService adminPostService;

    @MockitoBean
    private AdminCommentService adminCommentService;

    @MockitoBean
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        given(memberService.findBySso(any())).willReturn(Optional.of(authenticatedMember()));
    }

    @Test
    void injectsAuthenticatedMemberIntoMemberEndpoint() throws Exception {
        // given
        // 인증된 사용자의 프로필 응답을 준비한다.
        given(memberService.getMyProfile(1L))
                .willReturn(new MemberProfileResponse(1L, "comit-user", "2020111111", true, null));

        // when & then
        // 인증 헤더가 컨트롤러까지 주입되고 응답이 정상 직렬화되는지 확인한다.
        mockMvc.perform(get("/members/me")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "comit-user")
                        .header("X-Member-Student-Number", "2020111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.nickname").value("comit-user"))
                .andExpect(jsonPath("$.data.studentNumber").value("2020111111"));
    }

    @Test
    void returnsUnauthorizedWhenAuthenticationHeaderIsMissing() throws Exception {
        // when & then
        // 인증 헤더 없이 요청하면 401 ProblemDetail이 반환되어야 한다.
        mockMvc.perform(get("/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/common/unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void mapsPostCreateRequestUsingInterfaceAnnotationsAndAuthInjection() throws Exception {
        // given
        // 게시글 생성 서비스 응답을 준비한다.
        given(postService.createPost(eq(1L), any())).willReturn(10L);

        // when & then
        // 인터페이스 선언 기반 요청 매핑과 인증 주입이 함께 동작하는지 확인한다.
        mockMvc.perform(post("/posts")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "writer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardType": "FREE",
                                  "title": "첫 글",
                                  "content": "내용입니다.",
                                  "tags": ["spring", "boot"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(10L));
    }

    @Test
    void mapsPostDetailResponseIncludingViewCountUsingInterfaceAnnotationsAndAuthInjection() throws Exception {
        // given
        // 조회수까지 포함된 게시글 상세 응답을 준비한다.
        given(postService.getPost(eq(101L), eq(1L))).willReturn(
                new PostDetailResponse(
                        101L,
                        BoardType.QNA,
                        "JPA fetch join 질문",
                        "join fetch와 entity graph 차이가 궁금합니다.",
                        "backend-dev",
                        3,
                        128,
                        true,
                        List.of("spring", "jpa"),
                        List.of(),
                        LocalDateTime.parse("2026-03-24T10:00:00"),
                        LocalDateTime.parse("2026-03-24T10:30:00")
                )
        );

        // when & then
        // 상세 조회 응답이 문서 계약대로 직렬화되는지 확인한다.
        mockMvc.perform(get("/posts/101")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(101L))
                .andExpect(jsonPath("$.data.boardType").value("QNA"))
                .andExpect(jsonPath("$.data.viewCount").value(128))
                .andExpect(jsonPath("$.data.likedByMe").value(true))
                .andExpect(jsonPath("$.data.tags[0]").value("spring"));
    }

    @Test
    void mapsPostListResponseIncludingContentPreviewUsingInterfaceAnnotationsAndAuthInjection() throws Exception {
        // given
        given(postService.getPosts(eq(BoardType.QNA), eq(null), eq(20))).willReturn(
                new PostCursorPageResponse(
                        List.of(new PostSummaryResponse(
                                101L,
                                BoardType.QNA,
                                "JPA fetch join 질문",
                                "join fetch와 entity graph 차이가 궁금합니다.",
                                "backend-dev",
                                3,
                                2,
                                List.of("spring", "jpa"),
                                List.of("https://cdn.example.com/post-101/image-1.png"),
                                LocalDateTime.parse("2026-03-24T10:00:00")
                        )),
                        100L,
                        true
                )
        );

        // when & then
        mockMvc.perform(get("/posts")
                        .param("boardType", "QNA")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.posts[0].title").value("JPA fetch join 질문"))
                .andExpect(jsonPath("$.data.posts[0].contentPreview").value("join fetch와 entity graph 차이가 궁금합니다."))
                .andExpect(jsonPath("$.data.posts[0].imageUrls[0]").value("https://cdn.example.com/post-101/image-1.png"))
                .andExpect(jsonPath("$.data.nextCursorId").value(100L))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void mapsHotPostListResponseUsingInterfaceAnnotationsAndAuthInjection() throws Exception {
        // given
        // 인기글 목록 응답을 준비한다.
        given(postService.getHotPosts()).willReturn(new HotPostListResponse(List.of(
                new HotPostResponse(
                        1,
                        101L,
                        BoardType.QNA,
                        "JPA fetch join 질문",
                        "backend-dev",
                        3,
                        4,
                        List.of("spring", "jpa"),
                        LocalDateTime.parse("2026-03-24T10:00:00")
                )
        )));

        // when & then
        // 인기글 전용 응답 구조가 rank 포함 형태로 내려가는지 확인한다.
        mockMvc.perform(get("/posts/hot")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.posts[0].rank").value(1))
                .andExpect(jsonPath("$.data.posts[0].id").value(101L))
                .andExpect(jsonPath("$.data.posts[0].boardType").value("QNA"))
                .andExpect(jsonPath("$.data.posts[0].commentCount").value(4));
    }

    @Test
    void returnsMainPageWithoutAuthentication() throws Exception {
        // given
        // 메인 페이지 응답을 준비한다.
        given(mainService.getMainPage()).willReturn(new MainPageResponse(
                List.of(new PostSummaryResponse(
                        101L,
                        BoardType.QNA,
                        "QNA 최신글",
                        "본문 미리보기",
                        "writer",
                        3,
                        2,
                        List.of("spring"),
                        List.of("https://cdn.example.com/post-101.png"),
                        LocalDateTime.parse("2026-04-01T12:00:00")
                )),
                List.of(new PostSummaryResponse(
                        201L,
                        BoardType.INFO,
                        "INFO 최신글",
                        "본문 미리보기",
                        "writer",
                        3,
                        2,
                        List.of("info"),
                        List.of("https://cdn.example.com/post-201.png"),
                        LocalDateTime.parse("2026-04-01T12:00:00")
                )),
                List.of(new PostSummaryResponse(
                        301L,
                        BoardType.FREE,
                        "FREE 최신글",
                        "본문 미리보기",
                        "writer",
                        3,
                        2,
                        List.of("free"),
                        List.of("https://cdn.example.com/post-301.png"),
                        LocalDateTime.parse("2026-04-01T12:00:00")
                )),
                List.of(new PostSummaryResponse(
                        401L,
                        BoardType.NOTICE,
                        "NOTICE 최신글",
                        "본문 미리보기",
                        "writer",
                        3,
                        2,
                        List.of("notice"),
                        List.of("https://cdn.example.com/post-401.png"),
                        LocalDateTime.parse("2026-04-01T12:00:00")
                )),
                List.of(new PostSummaryResponse(
                        501L,
                        BoardType.EVENT,
                        "EVENT 최신글",
                        "본문 미리보기",
                        "writer",
                        3,
                        2,
                        List.of("event"),
                        List.of("https://cdn.example.com/post-501.png"),
                        LocalDateTime.parse("2026-04-01T12:00:00")
                )),
                List.of(new HotPostResponse(
                        1,
                        901L,
                        BoardType.QNA,
                        "인기글",
                        "writer",
                        11,
                        4,
                        List.of("spring"),
                        LocalDateTime.parse("2026-04-01T12:00:00")
                ))
        ));

        // when & then
        // 인증 헤더 없이 호출해도 200과 배열 형태의 섹션이 내려가야 한다.
        mockMvc.perform(get("/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.qna").isArray())
                .andExpect(jsonPath("$.data.info").isArray())
                .andExpect(jsonPath("$.data.free").isArray())
                .andExpect(jsonPath("$.data.notice").isArray())
                .andExpect(jsonPath("$.data.event").isArray())
                .andExpect(jsonPath("$.data.hotPosts").isArray())
                .andExpect(jsonPath("$.data.qna[0].title").value("QNA 최신글"))
                .andExpect(jsonPath("$.data.hotPosts[0].rank").value(1))
                .andExpect(jsonPath("$.data.hotPosts[0].id").value(901L));
    }

    @Test
    void mapsCommentCreateRequestUsingPathVariableAndRequestBodyFromInterfaceAnnotations() throws Exception {
        // given
        // 댓글 생성 서비스 응답을 준비한다.
        given(commentService.createComment(eq(10L), eq(1L), any())).willReturn(77L);

        // when & then
        // path variable과 request body가 선언된 계약대로 매핑되는지 확인한다.
        mockMvc.perform(post("/posts/10/comments")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "commenter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "좋은 글입니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(77L));
    }

    @Test
    void mapsPostReportRequestUsingInterfaceAnnotationsAndAuthInjection() throws Exception {
        // given
        // 게시글 신고 서비스 응답을 준비한다.
        given(reportService.reportPost(10L, 1L, "광고성 도배입니다")).willReturn(301L);

        // when & then
        // 신고 요청이 201 응답과 reportId로 직렬화되는지 확인한다.
        mockMvc.perform(post("/posts/10/reports")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "reporter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "광고성 도배입니다"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.reportId").value(301L));
    }

    @Test
    void mapsCommentReportRequestUsingInterfaceAnnotationsAndAuthInjection() throws Exception {
        // given
        // 댓글 신고 서비스 응답을 준비한다.
        given(reportService.reportComment(20L, 1L, "욕설이 포함되어 있습니다")).willReturn(302L);

        // when & then
        // 댓글 신고 요청이 201 응답과 reportId로 직렬화되는지 확인한다.
        mockMvc.perform(post("/comments/20/reports")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "reporter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "욕설이 포함되어 있습니다"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.reportId").value(302L));
    }

    @Test
    void mapsAdminReportListResponseForAdmin() throws Exception {
        // given
        // 관리자 신고 목록 응답을 준비한다.
        given(adminReportService.getReports(eq(null), eq(null), any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new AdminReportPageResponse(
                        List.of(new AdminReportSummaryResponse(
                                1L,
                                ReportTargetType.POST,
                                10L,
                                "광고성 도배입니다",
                                "reporter-1",
                                ReportStatus.RECEIVED,
                                LocalDateTime.parse("2026-03-28T10:00:00")
                        )),
                        0,
                        20,
                        1,
                        1
                ));

        // when & then
        // 관리자 요청이면 신고 목록 응답 구조가 정상 직렬화되어야 한다.
        mockMvc.perform(get("/admin/reports")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "admin")
                        .header("X-Member-Role", "ADMIN")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.reports[0].id").value(1L))
                .andExpect(jsonPath("$.data.reports[0].targetType").value("POST"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void returnsForbiddenWhenNonAdminRequestsAdminReportList() throws Exception {
        // when & then
        // 관리자 권한이 아니면 신고 관리자 API 접근이 거부되어야 한다.
        mockMvc.perform(get("/admin/reports")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "student")
                        .header("X-Member-Role", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/common/forbidden"))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void returnsForbiddenWhenNonAdminRequestsAdminMemberList() throws Exception {
        // when & then
        // 관리자 권한이 아니면 회원 관리자 API 접근이 거부되어야 한다.
        mockMvc.perform(get("/admin/members")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "student")
                        .header("X-Member-Role", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/common/forbidden"))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void returnsForbiddenWhenNonAdminRequestsAdminMemberDelete() throws Exception {
        // when & then
        // 관리자 권한이 아니면 회원 삭제 관리자 API 접근이 거부되어야 한다.
        mockMvc.perform(delete("/admin/members/1")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "student")
                        .header("X-Member-Role", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/common/forbidden"))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void returnsForbiddenWhenNonAdminRequestsAdminPostList() throws Exception {
        // when & then
        // 관리자 권한이 아니면 게시글 관리자 API 접근이 거부되어야 한다.
        mockMvc.perform(get("/admin/posts")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "student")
                        .header("X-Member-Role", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/common/forbidden"))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void returnsForbiddenWhenNonAdminRequestsAdminCommentList() throws Exception {
        // when & then
        // 관리자 권한이 아니면 댓글 관리자 API 접근이 거부되어야 한다.
        mockMvc.perform(get("/admin/comments")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "student")
                        .header("X-Member-Role", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/common/forbidden"))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void returnsBadRequestWhenAdminMemberStatusBodyOmitsStatus() throws Exception {
        // given
        // 실제 회원 조회까지 도달하도록 관리자 대상 회원을 준비한다.
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(authenticatedMember()));

        // when & then
        // status가 누락되면 요청 경계에서 INVALID_REQUEST로 거부되어야 한다.
        mockMvc.perform(patch("/admin/members/1/status")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "admin")
                        .header("X-Member-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "suspendedUntil": "2026-04-28T10:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/common/invalid-request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void deletesMemberWhenAdminRequestsAdminMemberDelete() throws Exception {
        // given
        // 실제 회원 조회까지 도달하도록 관리자 대상 회원을 준비한다.
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(authenticatedMember()));

        // when & then
        // 관리자 회원 삭제 요청은 성공 응답을 반환해야 한다.
        mockMvc.perform(delete("/admin/members/1")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "admin")
                        .header("X-Member-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    void returnsNotFoundWhenAdminRequestsMissingAdminMemberDelete() throws Exception {
        // given
        // 삭제 대상 회원이 존재하지 않는 상황을 준비한다.
        given(memberRepository.findByIdAndDeletedAtIsNull(99L)).willReturn(Optional.empty());

        // when & then
        // 존재하지 않는 회원 삭제 요청은 MEMBER_NOT_FOUND ProblemDetail로 변환되어야 한다.
        mockMvc.perform(delete("/admin/members/99")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "admin")
                        .header("X-Member-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/member/not-found"))
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestWhenAdminRequestsReceivedTransitionForReportReview() throws Exception {
        // given
        // RECEIVED로의 자기 전이를 요청하면 INVALID_REQUEST를 반환하도록 준비한다.
        willThrow(new BusinessException(CommonErrorCode.INVALID_REQUEST))
                .given(adminReportService)
                .reviewReport(1L, 1L, ReportStatus.RECEIVED);

        // when & then
        // 관리자 상태 변경 요청이 도메인 규칙 위반이면 400으로 변환되어야 한다.
        mockMvc.perform(patch("/admin/reports/1/status")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "admin")
                        .header("X-Member-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RECEIVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/common/invalid-request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void mapsReplyCreateRequestUsingOptionalParentCommentId() throws Exception {
        // given
        // parentCommentId가 포함된 대댓글 생성 응답을 준비한다.
        given(commentService.createComment(eq(10L), eq(1L), any())).willReturn(78L);

        // when & then
        // 선택 필드인 parentCommentId가 요청 본문에서 정상 매핑되는지 확인한다.
        mockMvc.perform(post("/posts/10/comments")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "commenter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentCommentId": 201,
                                  "content": "대댓글입니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(78L));
    }

    @Test
    void mapsNestedCommentListResponseUsingInterfaceAnnotations() throws Exception {
        // given
        // 부모 댓글과 대댓글이 함께 포함된 응답을 준비한다.
        given(commentService.getComments(eq(10L), eq(1L))).willReturn(
                new CommentListResponse(List.of(
                        new CommentResponse(
                                201L,
                                "부모 댓글",
                                "orm-master",
                                4,
                                true,
                                false,
                                LocalDateTime.parse("2026-03-24T11:00:00"),
                                null,
                                List.of(new ReplyResponse(
                                        202L,
                                        "대댓글입니다.",
                                        "backend-dev",
                                        1,
                                        false,
                                        true,
                                        LocalDateTime.parse("2026-03-24T11:05:00"),
                                        null
                                ))
                        )
                ))
        );

        // when & then
        // replies 중첩 구조가 JSON 응답으로 올바르게 직렬화되는지 확인한다.
        mockMvc.perform(get("/posts/10/comments")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.comments[0].id").value(201L))
                .andExpect(jsonPath("$.data.comments[0].replies[0].id").value(202L))
                .andExpect(jsonPath("$.data.comments[0].replies[0].mine").value(true));
    }

    @Test
    void validatesRequestBodyDeclaredOnInterface() throws Exception {
        // when & then
        // 인터페이스에 선언한 Bean Validation 규칙이 실제 요청 검증에 반영되는지 확인한다.
        mockMvc.perform(patch("/members/me")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "editor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/common/invalid-request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.invalidFields[0].field").value("nickname"));
    }

    @Test
    void validatesPostCreateLengthConstraintsDeclaredOnInterface() throws Exception {
        // given
        // 제목과 본문 길이 제한을 초과하는 요청을 준비한다.
        String tooLongTitle = "가".repeat(PostConstraints.TITLE_MAX_LENGTH + 1);
        String tooLongContent = "나".repeat(PostConstraints.CONTENT_MAX_LENGTH + 1);

        // when & then
        // 길이 제한 위반이 INVALID_REQUEST로 변환되는지 확인한다.
        mockMvc.perform(post("/posts")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "writer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardType": "FREE",
                                  "title": "%s",
                                  "content": "%s",
                                  "tags": []
                                }
                                """.formatted(tooLongTitle, tooLongContent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/common/invalid-request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.invalidFields[*].field", containsInAnyOrder("title", "content")));
    }

    @Test
    void validatesReportMessageDeclaredOnInterface() throws Exception {
        // when & then
        // 신고 메시지가 공백이면 Bean Validation에 의해 INVALID_REQUEST가 반환되어야 한다.
        mockMvc.perform(post("/posts/10/reports")
                        .header("X-Member-Sub", "member-1")
                        .header("X-Member-Name", "reporter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/common/invalid-request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.invalidFields[0].field").value("message"));
    }

    @Test
    void allowsConfiguredCorsOriginForFrontend() throws Exception {
        // when & then
        // 허용된 프론트 origin의 preflight 요청에는 CORS 헤더가 포함되어야 한다.
        mockMvc.perform(options("/members/me")
                        .header("Origin", "https://knu-cse-comit-client.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://knu-cse-comit-client.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void redirectsApiDocsRootToIndexHtml() throws Exception {
        // when & then
        // 문서 루트 경로는 index.html로 리다이렉트되어야 한다.
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/api/docs/index.html"));
    }

    @Test
    void redirectsStrippedDocsRootToIndexHtml() throws Exception {
        // when & then
        // 프록시가 /api prefix를 제거한 경로도 같은 문서 루트로 처리되어야 한다.
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/docs/index.html"));
    }

    @Test
    void servesGeneratedApiDocsIndexScript() throws Exception {
        // when & then
        // 생성된 API 문서 정적 산출물이 앱 경로로 서빙되어야 한다.
        mockMvc.perform(get("/api/docs/index.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("window.API_DOCS")));
    }

    @Test
    void servesGeneratedApiDocsIndexScriptOnStrippedDocsPath() throws Exception {
        // when & then
        // 프록시가 /api prefix를 제거한 경로에서도 정적 문서가 그대로 서빙되어야 한다.
        mockMvc.perform(get("/docs/index.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("window.API_DOCS")));
    }

    private Member authenticatedMember() {
        Member member = Member.create(
                "member-1",
                "테스트유저",
                "010-0000-0000",
                "comit-user",
                "2020111111",
                null,
                null,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }
}
