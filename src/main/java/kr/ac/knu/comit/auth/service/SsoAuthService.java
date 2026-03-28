package kr.ac.knu.comit.auth.service;

import java.util.UUID;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.dto.SsoCallbackSuccess;
import kr.ac.knu.comit.auth.dto.SsoLoginStart;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SsoAuthService {

    private final ComitSsoProperties ssoProperties;
    private final ExternalAuthClient externalAuthClient;
    private final AuthCookieManager authCookieManager;

    public SsoLoginStart startLogin() {
        validateFrontendSuccessUrl();

        String state = UUID.randomUUID().toString();
        String loginUrl = externalAuthClient.buildLoginRedirectUrl(state);

        return new SsoLoginStart(loginUrl, authCookieManager.createStateCookie(state));
    }

    public SsoCallbackSuccess handleCallback(String state, String token, String storedState) {
        validateFrontendSuccessUrl();
        validateState(state, storedState);
        externalAuthClient.verify(token);

        return new SsoCallbackSuccess(
                ssoProperties.getFrontendSuccessUrl(),
                authCookieManager.createTokenCookie(token),
                authCookieManager.clearStateCookie()
        );
    }

    public String clearAuthenticationCookie() {
        return authCookieManager.clearAuthenticationCookie();
    }

    private void validateState(String state, String storedState) {
        if (state == null || storedState == null || !state.equals(storedState)) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private void validateFrontendSuccessUrl() {
        if (isBlank(ssoProperties.getFrontendSuccessUrl())) {
            throw new BusinessException("SSO 설정이 올바르지 않습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
