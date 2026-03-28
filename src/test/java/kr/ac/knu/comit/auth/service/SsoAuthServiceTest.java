package kr.ac.knu.comit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.dto.SsoCallbackRejected;
import kr.ac.knu.comit.auth.dto.SsoCallbackResult;
import kr.ac.knu.comit.auth.dto.SsoCallbackSuccess;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
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
    @InjectMocks private SsoAuthService ssoAuthService;

    @Nested
    @DisplayName("handleCallback")
    class HandleCallback {

        @Test
        @DisplayName("CSE 학생이면 토큰 쿠키를 세팅하고 성공 URL로 리디렉션한다")
        void returnsSuccessForCseStudent() {
            // given
            ExternalIdentity identity = new ExternalIdentity("sub-1", "홍길동", "hong@knu.ac.kr", "2023012780", "CSE_STUDENT", "STUDENT");
            MemberPrincipal principal = new MemberPrincipal(null, "sub-1", "홍길동", "hong@knu.ac.kr", "2023012780",
                    MemberPrincipal.UserType.CSE_STUDENT, MemberPrincipal.MemberRole.STUDENT);

            given(ssoProperties.getFrontendSuccessUrl()).willReturn("https://comit.knu.ac.kr/home");
            given(externalAuthClient.verify("valid-token")).willReturn(identity);
            given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal);
            given(authCookieManager.createTokenCookie("valid-token")).willReturn("Set-Cookie: token=abc");
            given(authCookieManager.clearStateCookie()).willReturn("Set-Cookie: state=; Max-Age=0");

            // when
            SsoCallbackResult result = ssoAuthService.handleCallback("state-1", "valid-token", "state-1");

            // then
            assertThat(result).isInstanceOf(SsoCallbackSuccess.class);
            SsoCallbackSuccess success = (SsoCallbackSuccess) result;
            assertThat(success.redirectUrl()).isEqualTo("https://comit.knu.ac.kr/home");
            assertThat(success.tokenCookieHeader()).isEqualTo("Set-Cookie: token=abc");
        }

        @Test
        @DisplayName("외부인이면 토큰 쿠키 없이 에러 URL로 리디렉션한다")
        void returnsRejectedForExternalUser() {
            // given
            ExternalIdentity identity = new ExternalIdentity("sub-2", "외부인", "ext@gmail.com", null, "EXTERNAL", "STUDENT");
            MemberPrincipal principal = new MemberPrincipal(null, "sub-2", "외부인", "ext@gmail.com", null,
                    MemberPrincipal.UserType.EXTERNAL, MemberPrincipal.MemberRole.STUDENT);

            given(ssoProperties.getFrontendSuccessUrl()).willReturn("https://comit.knu.ac.kr/home");
            given(ssoProperties.getFrontendErrorUrl()).willReturn("https://comit.knu.ac.kr/error");
            given(externalAuthClient.verify("valid-token")).willReturn(identity);
            given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal);
            given(authCookieManager.clearStateCookie()).willReturn("Set-Cookie: state=; Max-Age=0");

            // when
            SsoCallbackResult result = ssoAuthService.handleCallback("state-1", "valid-token", "state-1");

            // then
            assertThat(result).isInstanceOf(SsoCallbackRejected.class);
            SsoCallbackRejected rejected = (SsoCallbackRejected) result;
            assertThat(rejected.redirectUrl()).isEqualTo("https://comit.knu.ac.kr/error?reason=EXTERNAL_USER_NOT_ALLOWED");
        }

        @Test
        @DisplayName("state가 불일치하면 INVALID_REQUEST 예외를 던진다")
        void throwsWhenStateDoesNotMatch() {
            // given
            given(ssoProperties.getFrontendSuccessUrl()).willReturn("https://comit.knu.ac.kr/home");

            // when & then
            assertThatThrownBy(() -> ssoAuthService.handleCallback("state-1", "token", "wrong-state"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }
}
