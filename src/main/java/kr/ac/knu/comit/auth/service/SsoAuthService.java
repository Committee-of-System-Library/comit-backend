package kr.ac.knu.comit.auth.service;

import java.net.URI;
import java.util.UUID;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SsoAuthService {

    private static final String ADMIN_DISPLAY = "관리자";
    private static final String ADMIN_PHONE_PLACEHOLDER = "000-000-0000";

    private final ComitSsoProperties ssoProperties;
    private final ExternalAuthClient externalAuthClient;
    private final AuthCookieManager authCookieManager;
    private final ExternalIdentityMapper externalIdentityMapper;
    private final MemberService memberService;
    private final MemberRegistrationService memberRegistrationService;
    private final AdminEmailProperties adminEmailProperties;

    public SsoLoginStart startLogin() {
        return startLogin(null);
    }

    public SsoLoginStart startLogin(String redirectUri) {
        validateFrontendUrls();

        String state = UUID.randomUUID().toString();
        String loginUrl = externalAuthClient.buildLoginRedirectUrl(state);
        String redirectUriCookieHeader = createRedirectUriCookieHeader(redirectUri);

        return new SsoLoginStart(loginUrl, authCookieManager.createStateCookie(state), redirectUriCookieHeader);
    }

    public SsoCallbackResult handleCallback(String state, String token, String storedState, String storedRedirectUri) {
        validateFrontendUrls();
        validateState(state, storedState);
        validateStoredRedirectUri(storedRedirectUri);

        ExternalIdentity identity = externalAuthClient.verify(token);
        MemberPrincipal principal = externalIdentityMapper.toPrincipal(identity);

        if (principal.userType() == MemberPrincipal.UserType.EXTERNAL) {
            return new SsoCallbackRejected(
                    resolveErrorUrl(storedRedirectUri, "EXTERNAL_USER_NOT_ALLOWED"),
                    authCookieManager.clearStateCookie(),
                    authCookieManager.clearRedirectUriCookie()
            );
        }

        if (memberService.hasDeletedMember(principal.ssoSub())) {
            return new SsoCallbackRejected(
                    resolveErrorUrl(storedRedirectUri, "ACCOUNT_DEACTIVATED"),
                    authCookieManager.clearStateCookie(),
                    authCookieManager.clearRedirectUriCookie()
            );
        }

        if (!memberService.hasActiveMember(principal.ssoSub())) {
            if (adminEmailProperties.isAdminEmail(identity.email())) {
                memberRegistrationService.register(
                        identity.ssoSub(),
                        ADMIN_DISPLAY,
                        ADMIN_PHONE_PLACEHOLDER,
                        adminNickname(identity.ssoSub()),
                        null,
                        null,
                        null
                );
            } else {
                return new SsoCallbackPendingRegistration(
                        resolveRegisterUrl(storedRedirectUri),
                        authCookieManager.createTokenCookie(token),
                        authCookieManager.clearStateCookie(),
                        authCookieManager.clearRedirectUriCookie()
                );
            }
        }

        return new SsoCallbackSuccess(
                resolveSuccessUrl(storedRedirectUri),
                authCookieManager.createTokenCookie(token),
                authCookieManager.clearStateCookie(),
                authCookieManager.clearRedirectUriCookie()
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

    private String createRedirectUriCookieHeader(String redirectUri) {
        if (isBlank(redirectUri)) {
            return authCookieManager.clearRedirectUriCookie();
        }
        validateRedirectUri(redirectUri);
        return authCookieManager.createRedirectUriCookie(redirectUri);
    }

    private void validateStoredRedirectUri(String storedRedirectUri) {
        if (!isBlank(storedRedirectUri)) {
            validateRedirectUri(storedRedirectUri);
        }
    }

    private void validateRedirectUri(String redirectUri) {
        try {
            URI uri = URI.create(redirectUri);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                throw new IllegalArgumentException("redirectUri must be absolute");
            }

            boolean isHttps = "https".equals(scheme);
            boolean isLocalhost = "http".equals(scheme) && "localhost".equals(host);
            if (!isHttps && !isLocalhost) {
                throw new IllegalArgumentException("redirectUri scheme is not allowed");
            }

            String origin = buildOrigin(uri);
            boolean allowed = ssoProperties.getAllowedRedirectUris().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .anyMatch(origin::equals);
            if (!allowed) {
                throw new IllegalArgumentException("redirectUri is not in allowlist");
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private String resolveSuccessUrl(String storedRedirectUri) {
        if (isBlank(storedRedirectUri)) {
            return ssoProperties.getFrontendSuccessUrl();
        }

        return buildStageRedirectUrl(storedRedirectUri, "success");
    }

    private String resolveRegisterUrl(String storedRedirectUri) {
        if (isBlank(storedRedirectUri)) {
            return ssoProperties.getFrontendRegisterUrl();
        }

        return buildStageRedirectUrl(storedRedirectUri, "register");
    }

    private String resolveErrorUrl(String storedRedirectUri, String reason) {
        if (isBlank(storedRedirectUri)) {
            return buildFrontendErrorRedirectUrl(reason);
        }

        return UriComponentsBuilder.fromUriString(storedRedirectUri)
                .queryParam("stage", "error")
                .queryParam("reason", reason)
                .build(true)
                .toUriString();
    }

    private String buildStageRedirectUrl(String redirectUri, String stage) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("stage", stage)
                .build(true)
                .toUriString();
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

    private String buildOrigin(URI uri) {
        int port = uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + (port != -1 ? ":" + port : "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String adminNickname(String ssoSub) {
        String cleaned = ssoSub.replaceAll("[^a-zA-Z0-9]", "");
        String suffix = cleaned.length() >= 6 ? cleaned.substring(0, 6) : cleaned;
        return "관리자-" + suffix;
    }
}
