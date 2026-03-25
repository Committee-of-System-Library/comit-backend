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
import kr.ac.knu.comit.post.dto.PostDetailResponse;
import kr.ac.knu.comit.post.service.PostService;
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

    @BeforeEach
    void setUp() {
        given(memberService.findOrCreateBySso(any())).willReturn(authenticatedMember());
    }

    @Test
    void injectsAuthenticatedMemberIntoMemberEndpoint() throws Exception {
        given(memberService.getMyProfile(1L))
                .willReturn(new MemberProfileResponse(1L, "comit-user", "2020111111", true));

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
        mockMvc.perform(get("/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/common/unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void mapsPostCreateRequestUsingInterfaceAnnotationsAndAuthInjection() throws Exception {
        given(postService.createPost(eq(1L), any())).willReturn(10L);

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
    void mapsCommentCreateRequestUsingPathVariableAndRequestBodyFromInterfaceAnnotations() throws Exception {
        given(commentService.createComment(eq(10L), eq(1L), any())).willReturn(77L);

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
    void validatesRequestBodyDeclaredOnInterface() throws Exception {
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
        String tooLongTitle = "가".repeat(PostConstraints.TITLE_MAX_LENGTH + 1);
        String tooLongContent = "나".repeat(PostConstraints.CONTENT_MAX_LENGTH + 1);

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

    private Member authenticatedMember() {
        Member member = Member.create("member-1", "comit-user", "2020111111");
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }
}
