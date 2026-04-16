package kr.ac.knu.comit.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import kr.ac.knu.comit.auth.config.AdminEmailProperties;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.auth.service.AuthCookieManager;
import kr.ac.knu.comit.auth.service.ExternalIdentityMapper;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;

import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "comit.auth.sso", name = "enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class SsoAuthenticationFilter extends OncePerRequestFilter {

    private static final String ADMIN_DISPLAY = "관리자";
    private static final String ADMIN_PHONE_PLACEHOLDER = "000-000-0000";

    private final AuthCookieManager authCookieManager;
    private final ExternalAuthClient externalAuthClient;
    private final ExternalIdentityMapper externalIdentityMapper;
    private final MemberService memberService;
    private final MemberRegistrationService memberRegistrationService;
    private final AdminEmailProperties adminEmailProperties;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getAttribute(MemberArgumentResolver.PRINCIPAL_ATTRIBUTE) instanceof MemberPrincipal) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authCookieManager.resolveTokenCookie(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ExternalIdentity identity = externalAuthClient.verify(token);
            MemberPrincipal provisionalPrincipal = externalIdentityMapper.toPrincipal(identity);

            Optional<Member> memberOptional = memberService.findBySso(provisionalPrincipal);
            if (memberOptional.isEmpty()) {
                if (adminEmailProperties.isAdminEmail(identity.email())) {
                    try {
                        memberRegistrationService.register(
                                identity.ssoSub(),
                                ADMIN_DISPLAY,
                                ADMIN_PHONE_PLACEHOLDER,
                                adminNickname(identity.ssoSub()),
                                null,
                                null,
                                null
                        );
                    } catch (BusinessException e) {
                        if (e.getErrorCode() != MemberErrorCode.MEMBER_ALREADY_EXISTS) {
                            throw e;
                        }
                    }
                    memberOptional = memberService.findBySso(provisionalPrincipal);
                } else {
                    throw new BusinessException(MemberErrorCode.REGISTRATION_REQUIRED);
                }
            }

            Member member = memberOptional.get();
            if (member.isBanned()) {
                throw new BusinessException(MemberErrorCode.MEMBER_BANNED);
            }
            if (member.isSuspended()) {
                throw new BusinessException(MemberErrorCode.MEMBER_SUSPENDED);
            }
            request.setAttribute(
                    MemberArgumentResolver.PRINCIPAL_ATTRIBUTE,
                    externalIdentityMapper.toPrincipal(member.getId(), identity)
            );
        } catch (BusinessException e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;
        } catch (Exception exception) {
            log.warn("Failed to authenticate SSO cookie for requestUri={}", request.getRequestURI(), exception);
            response.addHeader(HttpHeaders.SET_COOKIE, authCookieManager.clearAuthenticationCookie());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return isAuthPath(servletPath, "/auth/sso")
                || isAuthPath(servletPath, "/auth/register")
                || isAuthPath(servletPath, "/auth/dev");
    }

    private boolean isAuthPath(String servletPath, String path) {
        return servletPath.equals(path) || servletPath.startsWith(path + "/");
    }

    private static String adminNickname(String ssoSub) {
        String cleaned = ssoSub.replaceAll("[^a-zA-Z0-9]", "");
        String suffix = cleaned.length() >= 6 ? cleaned.substring(0, 6) : cleaned;
        return "관리자-" + suffix;
    }
}
