package kr.ac.knu.comit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthCookieManager")
class AuthCookieManagerTest {

    @Test
    @DisplayName("인증 쿠키 제거 시 설정된 token cookie와 legacy ACCESS_TOKEN, JSESSIONID를 함께 제거한다")
    void clearsConfiguredTokenCookieAndLegacyAccessTokenCookieAndSessionCookie() {
        AuthCookieManager authCookieManager = new AuthCookieManager(ssoProperties("COMIT_SSO_TOKEN"));

        List<String> cookies = authCookieManager.clearAuthenticationCookies();

        assertThat(cookies).hasSize(3);
        assertThat(cookies)
                .anySatisfy(cookie -> assertThat(cookie).contains("COMIT_SSO_TOKEN=").contains("Max-Age=0"));
        assertThat(cookies)
                .anySatisfy(cookie -> assertThat(cookie).contains("ACCESS_TOKEN=").contains("Max-Age=0"));
        assertThat(cookies)
                .anySatisfy(cookie -> assertThat(cookie).contains("JSESSIONID=").contains("Max-Age=0"));
    }

    @Test
    @DisplayName("설정된 token cookie가 ACCESS_TOKEN이면 ACCESS_TOKEN 제거 쿠키를 중복 생성하지 않는다")
    void doesNotDuplicateLegacyAccessTokenCookieWhenConfiguredTokenCookieIsAccessToken() {
        AuthCookieManager authCookieManager = new AuthCookieManager(ssoProperties("ACCESS_TOKEN"));

        List<String> cookies = authCookieManager.clearAuthenticationCookies();

        assertThat(cookies).hasSize(2);
        assertThat(cookies.getFirst()).contains("ACCESS_TOKEN=").contains("Max-Age=0");
        assertThat(cookies)
                .anySatisfy(cookie -> assertThat(cookie).contains("JSESSIONID=").contains("Max-Age=0"));
    }

    private ComitSsoProperties ssoProperties(String tokenCookieName) {
        ComitSsoProperties properties = new ComitSsoProperties();
        properties.setTokenCookieName(tokenCookieName);
        properties.setCookiePath("/");
        properties.setCookieSameSite("Lax");
        properties.setCookieSecure(false);
        return properties;
    }
}
