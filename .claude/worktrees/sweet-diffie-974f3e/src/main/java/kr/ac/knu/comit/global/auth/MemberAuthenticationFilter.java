package kr.ac.knu.comit.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import kr.ac.knu.comit.auth.config.AdminEmailProperties;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;

import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "comit.auth.bridge", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class MemberAuthenticationFilter extends OncePerRequestFilter {

    private static final String SUB_HEADER = "X-Member-Sub";
    private static final String NAME_HEADER = "X-Member-Name";
    private static final String EMAIL_HEADER = "X-Member-Email";
    private static final String STUDENT_NUMBER_HEADER = "X-Member-Student-Number";
    private static final String USER_TYPE_HEADER = "X-Member-User-Type";
    private static final String ROLE_HEADER = "X-Member-Role";

    private static final String ADMIN_DISPLAY = "관리자";
    private static final String ADMIN_PHONE_PLACEHOLDER = "000-000-0000";

    private final MemberService memberService;
    private final MemberRegistrationService memberRegistrationService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final AdminEmailProperties adminEmailProperties;

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

        String ssoSub = trimToNull(request.getHeader(SUB_HEADER));
        if (ssoSub == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = trimToNull(request.getHeader(EMAIL_HEADER));
        boolean isAdmin = adminEmailProperties.isAdminEmail(email);

        MemberPrincipal provisionalPrincipal = new MemberPrincipal(
                null,
                ssoSub,
                isAdmin ? ADMIN_DISPLAY : defaultName(request),
                isAdmin ? ADMIN_DISPLAY : email,
                trimToNull(request.getHeader(STUDENT_NUMBER_HEADER)),
                parseUserType(request.getHeader(USER_TYPE_HEADER)),
                isAdmin ? MemberPrincipal.MemberRole.ADMIN : parseRole(request.getHeader(ROLE_HEADER))
        );

        try {
            Optional<Member> memberOptional = memberService.findBySso(provisionalPrincipal);
            if (memberOptional.isEmpty()) {
                if (isAdmin) {
                    try {
                        memberRegistrationService.register(
                                ssoSub,
                                ADMIN_DISPLAY,
                                ADMIN_PHONE_PLACEHOLDER,
                                adminNickname(ssoSub),
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
            log.debug("[BridgeAuth] ssoSub={} comitRole={} uri={}",
                    member.getSsoSub(), member.getComitRole(), request.getRequestURI());
            MemberPrincipal authenticatedPrincipal = new MemberPrincipal(
                    member.getId(),
                    member.getSsoSub(),
                    provisionalPrincipal.name(),
                    provisionalPrincipal.email(),
                    provisionalPrincipal.studentNumber(),
                    provisionalPrincipal.userType(),
                    toMemberRole(member.getComitRole())
            );

            request.setAttribute(MemberArgumentResolver.PRINCIPAL_ATTRIBUTE, authenticatedPrincipal);
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return isAuthPath(requestUri, "/auth/sso")
                || isAuthPath(requestUri, "/auth/register")
                || isAuthPath(requestUri, "/auth/dev");
    }

    private boolean isAuthPath(String requestUri, String path) {
        return requestUri.equals(path) || requestUri.startsWith(path + "/");
    }

    private String defaultName(HttpServletRequest request) {
        String name = trimToNull(request.getHeader(NAME_HEADER));
        return name != null ? name : "comit-user";
    }

    private MemberPrincipal.UserType parseUserType(String rawValue) {
        return parseEnum(rawValue, MemberPrincipal.UserType.CSE_STUDENT, MemberPrincipal.UserType.class);
    }

    private MemberPrincipal.MemberRole toMemberRole(kr.ac.knu.comit.member.domain.ComitRole comitRole) {
        return comitRole == kr.ac.knu.comit.member.domain.ComitRole.ADMIN
                ? MemberPrincipal.MemberRole.ADMIN
                : MemberPrincipal.MemberRole.STUDENT;
    }

    private MemberPrincipal.MemberRole parseRole(String rawValue) {
        return parseEnum(rawValue, MemberPrincipal.MemberRole.STUDENT, MemberPrincipal.MemberRole.class);
    }

    private <T extends Enum<T>> T parseEnum(String rawValue, T defaultValue, Class<T> enumType) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String adminNickname(String ssoSub) {
        String cleaned = ssoSub.replaceAll("[^a-zA-Z0-9]", "");
        String suffix = cleaned.length() >= 6 ? cleaned.substring(0, 6) : cleaned;
        return "관리자-" + suffix;
    }
}
