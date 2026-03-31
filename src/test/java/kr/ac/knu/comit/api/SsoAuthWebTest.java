package kr.ac.knu.comit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.controller.SsoAuthController;
import kr.ac.knu.comit.auth.service.AuthCookieManager;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.auth.service.ExternalIdentityMapper;
import kr.ac.knu.comit.auth.service.SsoAuthService;
import kr.ac.knu.comit.global.auth.MemberArgumentResolver;
import kr.ac.knu.comit.global.auth.SsoAuthenticationFilter;
import kr.ac.knu.comit.global.config.WebMvcConfig;
import kr.ac.knu.comit.global.exception.GlobalExceptionHandler;
import kr.ac.knu.comit.member.controller.MemberController;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.dto.MemberProfileResponse;
import kr.ac.knu.comit.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest({SsoAuthController.class, MemberController.class})
@Import({
        WebMvcConfig.class,
        MemberArgumentResolver.class,
        GlobalExceptionHandler.class,
        ComitSsoProperties.class,
        AuthCookieManager.class,
        ExternalIdentityMapper.class,
        SsoAuthService.class,
        SsoAuthenticationFilter.class
})
@TestPropertySource(properties = {
        "comit.auth.bridge.enabled=false",
        "comit.auth.sso.enabled=true",
        "comit.auth.sso.auth-server-base-url=https://chcse.knu.ac.kr/appfn/api",
        "comit.auth.sso.client-id=cse-a1b2c3d4",
        "comit.auth.sso.client-secret=01234567890123456789012345678901",
        "comit.auth.sso.issuer=https://chcse.knu.ac.kr/appfn/api",
        "comit.auth.sso.redirect-uri=https://chcse.knu.ac.kr/comit-staging/api/auth/sso/callback",
        "comit.auth.sso.frontend-success-url=https://chcse.knu.ac.kr/comit-staging",
        "comit.auth.sso.frontend-error-url=https://chcse.knu.ac.kr/comit-staging/error",
        "comit.auth.sso.token-cookie-name=COMIT_SSO_TOKEN",
        "comit.auth.sso.state-cookie-name=COMIT_SSO_STATE",
        "comit.auth.sso.state-ttl-seconds=300",
        "comit.auth.sso.token-max-age-seconds=3600",
        "comit.auth.sso.cookie-path=/",
        "comit.auth.sso.cookie-secure=false",
        "comit.auth.sso.cookie-same-site=Lax"
})
@DisplayName("SsoAuthWeb")
class SsoAuthWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExternalAuthClient externalAuthClient;

    @MockitoBean
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        given(externalAuthClient.buildLoginRedirectUrl(any())).willReturn("https://chcse.knu.ac.kr/appfn/api/login?state=state-123");
        given(externalAuthClient.verify(any())).willReturn(externalIdentity());
        given(memberService.findOrCreateBySso(any())).willReturn(authenticatedMember());
        given(memberService.getMyProfile(1L))
                .willReturn(new MemberProfileResponse(1L, "comit-user", "2023012780", true));
    }

    @Test
    @DisplayName("로그인 시작 시 auth-server로 리다이렉트하고 state cookie를 발급한다")
    void redirectsToAuthServerAndSetsStateCookie() throws Exception {
        // given
        // SSO 로그인 시작에 필요한 설정이 준비된 상태다.

        // when
        // 로그인 시작 엔드포인트를 호출한다.
        MvcResult result = mockMvc.perform(get("/auth/sso/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://chcse.knu.ac.kr/appfn/api/login?")))
                .andReturn();

        // then
        // auth-server login URL로 이동하고 state cookie가 생성되어야 한다.
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_STATE="));
    }

    @Test
    @DisplayName("유효한 callback이면 token cookie를 심고 프론트로 리다이렉트한다")
    void setsTokenCookieAndRedirectsWhenCallbackIsValid() throws Exception {
        // given
        // auth-server가 전달한 valid state와 custom JWT를 준비한다.
        String token = "token-123";

        // when
        // callback 엔드포인트가 state와 token을 처리한다.
        MvcResult result = mockMvc.perform(get("/auth/sso/callback")
                        .param("state", "state-123")
                        .param("token", token)
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_STATE", "state-123")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://chcse.knu.ac.kr/comit-staging"))
                .andReturn();

        // then
        // SSO token cookie를 발급하고 state cookie는 제거되어야 한다.
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN="));
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_STATE=").contains("Max-Age=0"));
    }

    @Test
    @DisplayName("state가 다르면 callback을 거부한다")
    void rejectsCallbackWhenStateDoesNotMatch() throws Exception {
        // given
        // state cookie와 callback state가 다른 요청을 준비한다.
        String token = "token-123";

        // when
        // callback 엔드포인트를 잘못된 state로 호출한다.
        mockMvc.perform(get("/auth/sso/callback")
                        .param("state", "wrong-state")
                        .param("token", token)
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_STATE", "state-123")))

                // then
                // 요청이 잘못된 것으로 판단되어 400 ProblemDetail이 반환되어야 한다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("외부 SSO 사용자는 callback에서 에러 페이지로 리다이렉트한다")
    void redirectsExternalUserToFrontendErrorUrl() throws Exception {
        // given
        // EXTERNAL 사용자로 판정되는 SSO identity를 준비한다.
        given(externalAuthClient.verify(any())).willReturn(new ExternalIdentity(
                "external-sub",
                "external-user",
                "external-user@knu.ac.kr",
                "2023012780",
                "EXTERNAL",
                null
        ));

        // when
        // callback 엔드포인트를 EXTERNAL 사용자로 호출한다.
        MvcResult result = mockMvc.perform(get("/auth/sso/callback")
                        .param("state", "state-123")
                        .param("token", "token-123")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_STATE", "state-123")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://chcse.knu.ac.kr/comit-staging/error?reason=EXTERNAL_USER_NOT_ALLOWED"))
                .andReturn();

        // then
        // state cookie만 제거되고 token cookie는 발급되지 않아야 한다.
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_STATE=").contains("Max-Age=0"));
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .noneSatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN="));
    }

    @Test
    @DisplayName("유효한 SSO token cookie가 있으면 기존 인증 API가 그대로 동작한다")
    void authenticatesMemberEndpointUsingSsoTokenCookie() throws Exception {
        // given
        // auth-server custom JWT가 cookie에 저장된 상태를 준비한다.
        String token = "token-123";

        // when
        // 인증이 필요한 기존 member endpoint를 호출한다.
        mockMvc.perform(get("/members/me")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", token)))

                // then
                // 새 SSO 필터가 principal을 주입해 기존 API가 200으로 동작해야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.nickname").value("comit-user"))
                .andExpect(jsonPath("$.data.studentNumber").value("2023012780"));
    }

    @Test
    @DisplayName("잘못된 SSO token cookie는 제거하고 익명으로 처리한다")
    void clearsBadSsoCookieAndKeepsRequestAnonymous() throws Exception {
        // given
        // 검증 단계에서 실패하는 토큰을 준비한다.
        given(externalAuthClient.verify(any())).willThrow(new RuntimeException("invalid token"));

        // when
        // 인증이 필요한 기존 member endpoint를 잘못된 cookie와 함께 호출한다.
        MvcResult result = mockMvc.perform(get("/members/me")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", "bad-token")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // then
        // bad cookie는 제거되고 요청은 익명으로 계속 처리되어야 한다.
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN=").contains("Max-Age=0"));
    }

    @Test
    @DisplayName("로그아웃하면 SSO token cookie를 제거한다")
    void clearsSsoCookieOnLogout() throws Exception {
        // given
        // 이미 로그인되어 token cookie가 있는 상태를 가정한다.

        // when
        // 로그아웃 엔드포인트를 호출한다.
        MvcResult result = mockMvc.perform(post("/auth/sso/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        // then
        // token cookie가 만료되어야 한다.
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN=").contains("Max-Age=0"));
    }

    private Member authenticatedMember() {
        Member member = Member.create("sso-sub-1", "comit-user", "2023012780");
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private ExternalIdentity externalIdentity() {
        return new ExternalIdentity(
                "sso-sub-1",
                "comit-user",
                "comit-user@knu.ac.kr",
                "2023012780",
                "CSE_STUDENT",
                "STUDENT"
        );
    }
}
