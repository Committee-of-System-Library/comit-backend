package kr.ac.knu.comit.auth.service;

import java.util.UUID;
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
import kr.ac.knu.comit.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SsoAuthService {

    private final ComitSsoProperties ssoProperties;
    private final ExternalAuthClient externalAuthClient;
    private final AuthCookieManager authCookieManager;
    private final ExternalIdentityMapper externalIdentityMapper;
    private final MemberRepository memberRepository;

    public SsoLoginStart startLogin() {
        validateFrontendUrls();

        String state = UUID.randomUUID().toString();
        String loginUrl = externalAuthClient.buildLoginRedirectUrl(state);

        return new SsoLoginStart(loginUrl, authCookieManager.createStateCookie(state));
    }

    public SsoCallbackResult handleCallback(String state, String token, String storedState) {
        validateFrontendUrls();
        validateState(state, storedState);

        ExternalIdentity identity = externalAuthClient.verify(token);
        MemberPrincipal principal = externalIdentityMapper.toPrincipal(identity);

        if (principal.userType() == MemberPrincipal.UserType.EXTERNAL) {
            return new SsoCallbackRejected(
                    buildFrontendErrorRedirectUrl("EXTERNAL_USER_NOT_ALLOWED"),
                    authCookieManager.clearStateCookie()
            );
        }

        if (memberRepository.findBySsoSubAndDeletedAtIsNull(principal.ssoSub()).isEmpty()) {
            return new SsoCallbackPendingRegistration(
                    ssoProperties.getFrontendRegisterUrl(),
                    authCookieManager.createTokenCookie(token),
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

    private void validateFrontendErrorUrl() {
        if (isBlank(ssoProperties.getFrontendErrorUrl())) {
            throw new BusinessException("SSO 설정이 올바르지 않습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateFrontendRegisterUrl() {
        if (isBlank(ssoProperties.getFrontendRegisterUrl())) {
            throw new BusinessException("SSO 설정이 올바르지 않습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateFrontendUrls() {
        validateFrontendSuccessUrl();
        validateFrontendRegisterUrl();
        validateFrontendErrorUrl();
    }

    private String buildFrontendErrorRedirectUrl(String reason) {
        validateFrontendErrorUrl();

        try {
            return UriComponentsBuilder.fromUriString(ssoProperties.getFrontendErrorUrl())
                    .queryParam("reason", reason)
                    .build(true)
                    .toUriString();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("SSO 설정이 올바르지 않습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
