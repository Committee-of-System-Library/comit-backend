package kr.ac.knu.comit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.controller.RegisterController;
import kr.ac.knu.comit.auth.controller.SsoAuthController;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.auth.service.AuthCookieManager;
import kr.ac.knu.comit.auth.service.ExternalIdentityMapper;
import kr.ac.knu.comit.auth.service.RegisterService;
import kr.ac.knu.comit.auth.service.SsoAuthService;
import kr.ac.knu.comit.global.auth.MemberArgumentResolver;
import kr.ac.knu.comit.global.auth.SsoAuthenticationFilter;
import kr.ac.knu.comit.global.config.WebMvcConfig;
import kr.ac.knu.comit.global.exception.GlobalExceptionHandler;
import kr.ac.knu.comit.member.controller.MemberController;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.dto.MemberProfileResponse;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
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

@WebMvcTest({SsoAuthController.class, RegisterController.class, MemberController.class})
@Import({
        WebMvcConfig.class,
        MemberArgumentResolver.class,
        GlobalExceptionHandler.class,
        ComitSsoProperties.class,
        AuthCookieManager.class,
        ExternalIdentityMapper.class,
        RegisterService.class,
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
        "comit.auth.sso.frontend-register-url=https://chcse.knu.ac.kr/comit-staging/register",
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

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private MemberRegistrationService memberRegistrationService;

    @BeforeEach
    void setUp() {
        given(externalAuthClient.buildLoginRedirectUrl(any()))
                .willReturn("https://chcse.knu.ac.kr/appfn/api/login?state=state-123");
        given(externalAuthClient.verify(any())).willReturn(externalIdentity());
        given(memberService.findBySso(any())).willReturn(Optional.of(authenticatedMember()));
        given(memberService.getMyProfile(1L))
                .willReturn(new MemberProfileResponse(1L, "comit-user", "2023012780", true));
    }

    @Test
    @DisplayName("로그인 시작 시 auth-server로 리다이렉트하고 state cookie를 발급한다")
    void redirectsToAuthServerAndSetsStateCookie() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get("/auth/sso/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://chcse.knu.ac.kr/appfn/api/login?")))
                .andReturn();

        // then
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_STATE="));
    }

    @Test
    @DisplayName("유효한 callback이고 이미 가입된 회원이면 success URL로 리다이렉트한다")
    void setsTokenCookieAndRedirectsWhenCallbackIsValid() throws Exception {
        // given
        given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-sub-1"))
                .willReturn(Optional.of(authenticatedMember()));

        // when
        MvcResult result = mockMvc.perform(get("/auth/sso/callback")
                        .param("state", "state-123")
                        .param("token", "token-123")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_STATE", "state-123")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://chcse.knu.ac.kr/comit-staging"))
                .andReturn();

        // then
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN="));
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_STATE=").contains("Max-Age=0"));
    }

    @Test
    @DisplayName("유효한 callback이지만 미가입 회원이면 register URL로 리다이렉트한다")
    void redirectsToRegisterWhenMemberDoesNotExist() throws Exception {
        // given
        given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-sub-1")).willReturn(Optional.empty());

        // when
        MvcResult result = mockMvc.perform(get("/auth/sso/callback")
                        .param("state", "state-123")
                        .param("token", "token-123")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_STATE", "state-123")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://chcse.knu.ac.kr/comit-staging/register"))
                .andReturn();

        // then
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN="));
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_STATE=").contains("Max-Age=0"));
    }

    @Test
    @DisplayName("state가 다르면 callback을 거부한다")
    void rejectsCallbackWhenStateDoesNotMatch() throws Exception {
        mockMvc.perform(get("/auth/sso/callback")
                        .param("state", "wrong-state")
                        .param("token", "token-123")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_STATE", "state-123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("외부 SSO 사용자는 callback에서 에러 페이지로 리다이렉트한다")
    void redirectsExternalUserToFrontendErrorUrl() throws Exception {
        // given
        given(externalAuthClient.verify(any())).willReturn(new ExternalIdentity(
                "external-sub",
                "external-user",
                "external-user@knu.ac.kr",
                "2023012780",
                null,
                "EXTERNAL",
                null
        ));

        // when
        MvcResult result = mockMvc.perform(get("/auth/sso/callback")
                        .param("state", "state-123")
                        .param("token", "token-123")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_STATE", "state-123")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://chcse.knu.ac.kr/comit-staging/error?reason=EXTERNAL_USER_NOT_ALLOWED"))
                .andReturn();

        // then
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_STATE=").contains("Max-Age=0"));
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .noneSatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN="));
    }

    @Test
    @DisplayName("유효한 SSO token cookie가 있으면 기존 인증 API가 그대로 동작한다")
    void authenticatesMemberEndpointUsingSsoTokenCookie() throws Exception {
        mockMvc.perform(get("/members/me")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", "token-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.nickname").value("comit-user"))
                .andExpect(jsonPath("$.data.studentNumber").value("2023012780"));
    }

    @Test
    @DisplayName("미가입 상태에서 일반 API에 접근하면 REGISTRATION_REQUIRED를 반환한다")
    void blocksGeneralApiWhenRegistrationIsPending() throws Exception {
        // given
        given(memberService.findBySso(any())).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/members/me")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", "token-123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("REGISTRATION_REQUIRED"));
    }

    @Test
    @DisplayName("prefill API는 JWT에서 name, studentNumber, major를 반환한다")
    void returnsRegisterPrefillFromVerifiedToken() throws Exception {
        // given
        given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-sub-1")).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/auth/register/prefill")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", "token-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.studentNumber").value("2023012780"))
                .andExpect(jsonPath("$.data.major").value("심화"));
    }

    @Test
    @DisplayName("register API는 JWT claim과 요청 본문으로 회원가입을 완료한다")
    void registersMemberUsingTokenClaimsAndRequestBody() throws Exception {
        // given
        given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-sub-1")).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(post("/auth/register")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", "token-123"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "nickname": "길동이",
                                  "phone": "010-1234-5678",
                                  "agreedToTerms": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        then(memberRegistrationService).should().register(
                eq("sso-sub-1"),
                eq("홍길동"),
                eq("010-1234-5678"),
                eq("길동이"),
                eq("2023012780"),
                eq("심화")
        );
    }

    @Test
    @DisplayName("register API는 agreedToTerms가 false면 INVALID_REQUEST를 반환한다")
    void rejectsRegistrationWhenTermsAreNotAgreed() throws Exception {
        // given
        given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-sub-1")).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(post("/auth/register")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", "token-123"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "nickname": "길동이",
                                  "phone": "010-1234-5678",
                                  "agreedToTerms": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("잘못된 SSO token cookie는 제거하고 익명으로 처리한다")
    void clearsBadSsoCookieAndKeepsRequestAnonymous() throws Exception {
        // given
        given(externalAuthClient.verify(any())).willThrow(new RuntimeException("invalid token"));

        // when
        MvcResult result = mockMvc.perform(get("/members/me")
                        .cookie(new jakarta.servlet.http.Cookie("COMIT_SSO_TOKEN", "bad-token")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // then
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN=").contains("Max-Age=0"));
    }

    @Test
    @DisplayName("로그아웃하면 SSO token cookie를 제거한다")
    void clearsSsoCookieOnLogout() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/sso/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN=").contains("Max-Age=0"));
    }

    private Member authenticatedMember() {
        Member member = Member.create(
                "sso-sub-1",
                "테스트유저",
                "010-0000-0000",
                "comit-user",
                "2023012780",
                null,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private ExternalIdentity externalIdentity() {
        return new ExternalIdentity(
                "sso-sub-1",
                "홍길동",
                "comit-user@knu.ac.kr",
                "2023012780",
                "심화",
                "CSE_STUDENT",
                "STUDENT"
        );
    }
}
