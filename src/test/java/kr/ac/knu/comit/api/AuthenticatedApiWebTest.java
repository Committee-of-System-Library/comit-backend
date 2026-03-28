package kr.ac.knu.comit.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knu.comit.comment.controller.CommentController;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CommentResponse;
import kr.ac.knu.comit.comment.dto.ReplyResponse;
import kr.ac.knu.comit.comment.service.CommentService;
import kr.ac.knu.comit.global.auth.MemberArgumentResolver;
import kr.ac.knu.comit.global.auth.MemberAuthenticationFilter;
import kr.ac.knu.comit.global.config.WebMvcConfig;
import kr.ac.knu.comit.global.exception.GlobalExceptionHandler;
import kr.ac.knu.comit.member.controller.MemberController;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.dto.MemberProfileResponse;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.controller.PostController;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.PostConstraints;
import kr.ac.knu.comit.post.dto.HotPostListResponse;
import kr.ac.knu.comit.post.dto.HotPostResponse;
import kr.ac.knu.comit.post.dto.PostDetailResponse;
import kr.ac.knu.comit.post.service.PostService;
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

@WebMvcTest({MemberController.class, PostController.class, CommentController.class})
@Import({
        WebMvcConfig.class,
        MemberArgumentResolver.class,
        MemberAuthenticationFilter.class,
        GlobalExceptionHandler.class
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
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        given(memberService.findOrCreateBySso(any())).willReturn(authenticatedMember());
    }

    @Test
    void injectsAuthenticatedMemberIntoMemberEndpoint() throws Exception {
        // given
        // 인증된 사용자의 프로필 응답을 준비한다.
        given(memberService.getMyProfile(1L))
                .willReturn(new MemberProfileResponse(1L, "comit-user", "2020111111", true));

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

    private Member authenticatedMember() {
        Member member = Member.create("member-1", "comit-user", "2020111111");
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }
}
