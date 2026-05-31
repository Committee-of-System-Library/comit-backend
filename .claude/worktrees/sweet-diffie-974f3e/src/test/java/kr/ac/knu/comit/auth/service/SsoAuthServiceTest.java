package kr.ac.knu.comit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;

import java.util.List;
import kr.ac.knu.comit.auth.config.AdminEmailProperties;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.dto.SsoCallbackPendingRegistration;
import kr.ac.knu.comit.auth.dto.SsoCallbackRejected;
import kr.ac.knu.comit.auth.dto.SsoCallbackResult;
import kr.ac.knu.comit.auth.dto.SsoCallbackSuccess;
import kr.ac.knu.comit.auth.dto.SsoLoginStart;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SsoAuthService")
class SsoAuthServiceTest {

    @Mock private ComitSsoProperties ssoProperties;
    @Mock private ExternalAuthClient externalAuthClient;
    @Mock private AuthCookieManager authCookieManager;
    @Mock private ExternalIdentityMapper externalIdentityMapper;
    @Mock private MemberService memberService;
    @Mock private MemberRegistrationService memberRegistrationService;
    @Mock private AdminEmailProperties adminEmailProperties;
    @InjectMocks private SsoAuthService ssoAuthService;

    @Nested
    @DisplayName("startLogin")
    class StartLogin {

        @Test
        @DisplayName("allowlisted redirectUri가 있으면 redirectUri 쿠키를 발급하고 로그인 URL을 반환한다")
        void returnsRedirectUriCookieWhenRedirectUriIsAllowlisted() {
            // given
            givenFrontendUrls();
            given(ssoProperties.getAllowedRedirectUris())
                    .willReturn(List.of("https://comit-sso-smoke.vercel.app"));
            given(externalAuthClient.buildLoginRedirectUrl(any()))
                    .willReturn("https://chcse.knu.ac.kr/appfn/api/login?state=state-123");
            given(authCookieManager.createStateCookie(any()))
                    .willReturn("Set-Cookie: COMIT_SSO_STATE=state-123");
            given(authCookieManager.createRedirectUriCookie("https://comit-sso-smoke.vercel.app"))
                    .willReturn("Set-Cookie: comit-redirect-uri=https://comit-sso-smoke.vercel.app");

            // when
            SsoLoginStart result = ssoAuthService.startLogin("https://comit-sso-smoke.vercel.app");

            // then
            assertThat(result.loginUrl()).isEqualTo("https://chcse.knu.ac.kr/appfn/api/login?state=state-123");
            assertThat(result.stateCookieHeader()).isEqualTo("Set-Cookie: COMIT_SSO_STATE=state-123");
            assertThat(result.redirectUriCookieHeader())
                    .isEqualTo("Set-Cookie: comit-redirect-uri=https://comit-sso-smoke.vercel.app");
        }

        @Test
        @DisplayName("redirectUri가 없으면 stale redirectUri 쿠키를 제거한다")
        void clearsStaleRedirectUriCookieWhenRedirectUriIsMissing() {
            // given
            givenFrontendUrls();
            given(externalAuthClient.buildLoginRedirectUrl(any()))
                    .willReturn("https://chcse.knu.ac.kr/appfn/api/login?state=state-123");
            given(authCookieManager.createStateCookie(any()))
                    .willReturn("Set-Cookie: COMIT_SSO_STATE=state-123");
            given(authCookieManager.clearRedirectUriCookie())
                    .willReturn("Set-Cookie: comit-redirect-uri=; Max-Age=0");

            // when
            SsoLoginStart result = ssoAuthService.startLogin(null);

            // then
            assertThat(result.redirectUriCookieHeader())
                    .isEqualTo("Set-Cookie: comit-redirect-uri=; Max-Age=0");
        }

        @Test
        @DisplayName("allowlist 밖 redirectUri는 INVALID_REQUEST를 던진다")
        void rejectsRedirectUriOutsideAllowlist() {
            // given
            givenFrontendUrls();
            given(ssoProperties.getAllowedRedirectUris())
                    .willReturn(List.of("https://comit-sso-smoke.vercel.app"));

            // when & then
            assertThatThrownBy(() -> ssoAuthService.startLogin("https://evil.com"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("handleCallback")
    class HandleCallback {

        @Test
        @DisplayName("가입된 INTERNAL 사용자는 토큰 쿠키를 세팅하고 성공 URL로 리디렉션한다")
        void returnsSuccessForRegisteredMember() {
            // given
            ExternalIdentity identity = new ExternalIdentity(
                    "sub-1",
                    "홍길동",
                    "hong@knu.ac.kr",
                    "2023012780",
                    "심화",
                    "CSE_STUDENT",
                    "STUDENT"
            );
            MemberPrincipal principal = new MemberPrincipal(
                    null,
                    "sub-1",
                    "홍길동",
                    "hong@knu.ac.kr",
                    "2023012780",
                    MemberPrincipal.UserType.CSE_STUDENT,
                    MemberPrincipal.MemberRole.STUDENT
            );

            givenFrontendUrls();
            given(externalAuthClient.verify("valid-token")).willReturn(identity);
            given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal);
            given(memberService.hasActiveMember("sub-1")).willReturn(true);
            given(authCookieManager.createTokenCookie("valid-token")).willReturn("Set-Cookie: token=abc");
            given(authCookieManager.clearStateCookie()).willReturn("Set-Cookie: state=; Max-Age=0");
            given(authCookieManager.clearRedirectUriCookie())
                    .willReturn("Set-Cookie: comit-redirect-uri=; Max-Age=0");

            // when
            SsoCallbackResult result = ssoAuthService.handleCallback(
                    "state-1",
                    "valid-token",
                    "state-1",
                    "https://comit-sso-smoke.vercel.app"
            );

            // then
            assertThat(result).isInstanceOf(SsoCallbackSuccess.class);
            SsoCallbackSuccess success = (SsoCallbackSuccess) result;
            assertThat(success.redirectUrl()).isEqualTo("https://comit-sso-smoke.vercel.app?stage=success");
            assertThat(success.tokenCookieHeader()).isEqualTo("Set-Cookie: token=abc");
            assertThat(success.clearRedirectUriCookieHeader())
                    .isEqualTo("Set-Cookie: comit-redirect-uri=; Max-Age=0");
        }

        @Test
        @DisplayName("미가입 INTERNAL 사용자는 회원가입 URL로 리디렉션한다")
        void returnsPendingRegistrationWhenMemberDoesNotExist() {
            // given
            ExternalIdentity identity = new ExternalIdentity(
                    "sub-1",
                    "홍길동",
                    "hong@knu.ac.kr",
                    "2023012780",
                    "심화",
                    "CSE_STUDENT",
                    "STUDENT"
            );
            MemberPrincipal principal = new MemberPrincipal(
                    null,
                    "sub-1",
                    "홍길동",
                    "hong@knu.ac.kr",
                    "2023012780",
                    MemberPrincipal.UserType.CSE_STUDENT,
                    MemberPrincipal.MemberRole.STUDENT
            );

            givenFrontendUrls();
            given(externalAuthClient.verify("valid-token")).willReturn(identity);
            given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal);
            given(memberService.hasActiveMember("sub-1")).willReturn(false);
            given(authCookieManager.createTokenCookie("valid-token")).willReturn("Set-Cookie: token=abc");
            given(authCookieManager.clearStateCookie()).willReturn("Set-Cookie: state=; Max-Age=0");
            given(authCookieManager.clearRedirectUriCookie())
                    .willReturn("Set-Cookie: comit-redirect-uri=; Max-Age=0");

            // when
            SsoCallbackResult result = ssoAuthService.handleCallback(
                    "state-1",
                    "valid-token",
                    "state-1",
                    "https://comit-sso-smoke.vercel.app"
            );

            // then
            assertThat(result).isInstanceOf(SsoCallbackPendingRegistration.class);
            SsoCallbackPendingRegistration pendingRegistration = (SsoCallbackPendingRegistration) result;
            assertThat(pendingRegistration.redirectUrl()).isEqualTo("https://comit-sso-smoke.vercel.app?stage=register");
            assertThat(pendingRegistration.tokenCookie()).isEqualTo("Set-Cookie: token=abc");
            assertThat(pendingRegistration.clearRedirectUriCookieHeader())
                    .isEqualTo("Set-Cookie: comit-redirect-uri=; Max-Age=0");
        }

        @Test
        @DisplayName("soft delete 회원이 있으면 회원가입 URL 대신 에러 URL로 리디렉션한다")
        void returnsRejectedWhenDeletedMemberExists() {
            // given
            ExternalIdentity identity = new ExternalIdentity(
                    "sub-1",
                    "홍길동",
                    "hong@knu.ac.kr",
                    "2023012780",
                    "심화",
                    "CSE_STUDENT",
                    "STUDENT"
            );
            MemberPrincipal principal = new MemberPrincipal(
                    null,
                    "sub-1",
                    "홍길동",
                    "hong@knu.ac.kr",
                    "2023012780",
                    MemberPrincipal.UserType.CSE_STUDENT,
                    MemberPrincipal.MemberRole.STUDENT
            );

            givenFrontendUrls();
            given(externalAuthClient.verify("valid-token")).willReturn(identity);
            given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal);
            given(memberService.hasDeletedMember("sub-1")).willReturn(true);
            given(authCookieManager.clearStateCookie()).willReturn("Set-Cookie: state=; Max-Age=0");
            given(authCookieManager.clearRedirectUriCookie())
                    .willReturn("Set-Cookie: comit-redirect-uri=; Max-Age=0");

            // when
            SsoCallbackResult result = ssoAuthService.handleCallback(
                    "state-1",
                    "valid-token",
                    "state-1",
                    "https://comit-sso-smoke.vercel.app"
            );

            // then
            assertThat(result).isInstanceOf(SsoCallbackRejected.class);
            SsoCallbackRejected rejected = (SsoCallbackRejected) result;
            assertThat(rejected.redirectUrl())
                    .isEqualTo("https://comit-sso-smoke.vercel.app?stage=error&reason=ACCOUNT_DEACTIVATED");
            assertThat(rejected.clearRedirectUriCookieHeader())
                    .isEqualTo("Set-Cookie: comit-redirect-uri=; Max-Age=0");
        }

        @Test
        @DisplayName("외부인이면 토큰 쿠키 없이 에러 URL로 리디렉션한다")
        void returnsRejectedForExternalUser() {
            // given
            ExternalIdentity identity = new ExternalIdentity(
                    "sub-2",
                    "외부인",
                    "ext@gmail.com",
                    null,
                    null,
                    "EXTERNAL",
                    "STUDENT"
            );
            MemberPrincipal principal = new MemberPrincipal(
                    null,
                    "sub-2",
                    "외부인",
                    "ext@gmail.com",
                    null,
                    MemberPrincipal.UserType.EXTERNAL,
                    MemberPrincipal.MemberRole.STUDENT
            );

            givenFrontendUrls();
            given(externalAuthClient.verify("valid-token")).willReturn(identity);
            given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal);
            given(authCookieManager.clearStateCookie()).willReturn("Set-Cookie: state=; Max-Age=0");
            given(authCookieManager.clearRedirectUriCookie())
                    .willReturn("Set-Cookie: comit-redirect-uri=; Max-Age=0");

            // when
            SsoCallbackResult result = ssoAuthService.handleCallback(
                    "state-1",
                    "valid-token",
                    "state-1",
                    "https://comit-sso-smoke.vercel.app"
            );

            // then
            assertThat(result).isInstanceOf(SsoCallbackRejected.class);
            SsoCallbackRejected rejected = (SsoCallbackRejected) result;
            assertThat(rejected.redirectUrl())
                    .isEqualTo("https://comit-sso-smoke.vercel.app?stage=error&reason=EXTERNAL_USER_NOT_ALLOWED");
            assertThat(rejected.clearRedirectUriCookieHeader())
                    .isEqualTo("Set-Cookie: comit-redirect-uri=; Max-Age=0");
        }

        @Test
        @DisplayName("state가 불일치하면 INVALID_REQUEST 예외를 던진다")
        void throwsWhenStateDoesNotMatch() {
            // given
            givenFrontendUrls();

            // when & then
            assertThatThrownBy(() -> ssoAuthService.handleCallback("state-1", "token", "wrong-state", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private void givenFrontendUrls() {
        given(ssoProperties.getFrontendSuccessUrl()).willReturn("https://comit.knu.ac.kr/home");
        given(ssoProperties.getFrontendRegisterUrl()).willReturn("https://comit.knu.ac.kr/register");
        given(ssoProperties.getFrontendErrorUrl()).willReturn("https://comit.knu.ac.kr/error");
        lenient().when(ssoProperties.getAllowedRedirectUris())
                .thenReturn(List.of("https://comit-sso-smoke.vercel.app"));
        lenient().when(memberService.hasDeletedMember("sub-1")).thenReturn(false);
    }
}
