package kr.ac.knu.comit.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    private final ComitSsoProperties ssoProperties;

    public String createStateCookie(String state) {
        return ResponseCookie.from(ssoProperties.getStateCookieName(), state)
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(ssoProperties.getStateTtlSeconds())
                .build()
                .toString();
    }

    public String createTokenCookie(String token) {
        return ResponseCookie.from(ssoProperties.getTokenCookieName(), token)
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(ssoProperties.getTokenMaxAgeSeconds())
                .build()
                .toString();
    }

    public String createRedirectUriCookie(String redirectUri) {
        return ResponseCookie.from(ssoProperties.getRedirectUriCookieName(), redirectUri)
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(ssoProperties.getStateTtlSeconds())
                .build()
                .toString();
    }

    public String clearStateCookie() {
        return clearCookie(ssoProperties.getStateCookieName());
    }

    public String clearAuthenticationCookie() {
        return clearCookie(ssoProperties.getTokenCookieName());
    }

    public String clearRedirectUriCookie() {
        return clearCookie(ssoProperties.getRedirectUriCookieName());
    }

    public String resolveStateCookie(HttpServletRequest request) {
        return resolveCookie(request, ssoProperties.getStateCookieName());
    }

    public String resolveTokenCookie(HttpServletRequest request) {
        return resolveCookie(request, ssoProperties.getTokenCookieName());
    }

    public String resolveRedirectUriCookie(HttpServletRequest request) {
        return resolveCookie(request, ssoProperties.getRedirectUriCookieName());
    }

    private String clearCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(0)
                .build()
                .toString();
    }

    private String resolveCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String cookiePath() {
        String path = ssoProperties.getCookiePath();
        return (path == null || path.isBlank()) ? "/" : path;
    }
}
