package kr.ac.knu.comit.auth.service;

import java.util.UUID;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.dto.SsoCallbackSuccess;
import kr.ac.knu.comit.auth.dto.SsoLoginStart;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SsoAuthService {

    private final ComitSsoProperties ssoProperties;
    private final SsoTokenVerifier ssoTokenVerifier;

    public SsoLoginStart startLogin() {
        validateRequiredProperties();

        String state = UUID.randomUUID().toString();
        String loginUrl = UriComponentsBuilder.fromUriString(normalizeBaseUrl(ssoProperties.getAuthServerBaseUrl()) + "/login")
                .queryParam("client_id", ssoProperties.getClientId())
                .queryParam("redirect_uri", ssoProperties.getRedirectUri())
                .queryParam("state", state)
                .build(true)
                .toUriString();

        return new SsoLoginStart(loginUrl, stateCookie(state).toString());
    }

    public SsoCallbackSuccess handleCallback(String state, String token, String storedState) {
        validateRequiredProperties();
        validateState(state, storedState);
        ssoTokenVerifier.verify(token);

        return new SsoCallbackSuccess(
                ssoProperties.getFrontendSuccessUrl(),
                tokenCookie(token).toString(),
                clearStateCookie().toString()
        );
    }

    public String clearAuthenticationCookie() {
        return clearCookie(ssoProperties.getTokenCookieName()).toString();
    }

    private void validateState(String state, String storedState) {
        if (state == null || storedState == null || !state.equals(storedState)) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRequiredProperties() {
        if (isBlank(ssoProperties.getClientId())
                || isBlank(ssoProperties.getClientSecret())
                || isBlank(ssoProperties.getRedirectUri())
                || isBlank(ssoProperties.getFrontendSuccessUrl())
                || isBlank(ssoProperties.getIssuer())) {
            throw new BusinessException("SSO 설정이 올바르지 않습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseCookie stateCookie(String state) {
        return ResponseCookie.from(ssoProperties.getStateCookieName(), state)
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(ssoProperties.getStateTtlSeconds())
                .build();
    }

    private ResponseCookie tokenCookie(String token) {
        return ResponseCookie.from(ssoProperties.getTokenCookieName(), token)
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(ssoProperties.getTokenMaxAgeSeconds())
                .build();
    }

    private ResponseCookie clearStateCookie() {
        return clearCookie(ssoProperties.getStateCookieName());
    }

    private ResponseCookie clearCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(0)
                .build();
    }

    private String cookiePath() {
        String path = ssoProperties.getCookiePath();
        return isBlank(path) ? "/" : path;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) {
            throw new BusinessException("SSO 인증 서버 주소가 비어 있습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
