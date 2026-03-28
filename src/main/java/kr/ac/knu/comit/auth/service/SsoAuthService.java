package kr.ac.knu.comit.auth.service;

import java.util.UUID;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.dto.SsoCallbackRejected;
import kr.ac.knu.comit.auth.dto.SsoCallbackResult;
import kr.ac.knu.comit.auth.dto.SsoCallbackSuccess;
import kr.ac.knu.comit.auth.dto.SsoLoginStart;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
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
    private final ExternalIdentityMapper externalIdentityMapper;

    public SsoLoginStart startLogin() {
        validateFrontendSuccessUrl();

        String state = UUID.randomUUID().toString();
        String loginUrl = externalAuthClient.buildLoginRedirectUrl(state);

        return new SsoLoginStart(loginUrl, authCookieManager.createStateCookie(state));
    }

    public SsoCallbackResult handleCallback(String state, String token, String storedState) {
        validateFrontendSuccessUrl();
        validateState(state, storedState);

        ExternalIdentity identity = externalAuthClient.verify(token);
        MemberPrincipal principal = externalIdentityMapper.toPrincipal(identity);

        if (principal.userType() == MemberPrincipal.UserType.EXTERNAL) {
            return new SsoCallbackRejected(
                    ssoProperties.getFrontendErrorUrl() + "?reason=EXTERNAL_USER_NOT_ALLOWED",
                    authCookieManager.clearStateCookie()
            );
        }

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
