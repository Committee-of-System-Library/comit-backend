package kr.ac.knu.comit.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

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

    private final MemberService memberService;
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

        String ssoSub = trimToNull(request.getHeader(SUB_HEADER));
        if (ssoSub == null) {
            filterChain.doFilter(request, response);
            return;
        }

        MemberPrincipal provisionalPrincipal = new MemberPrincipal(
                null,
                ssoSub,
                defaultName(request),
                trimToNull(request.getHeader(EMAIL_HEADER)),
                trimToNull(request.getHeader(STUDENT_NUMBER_HEADER)),
                parseUserType(request.getHeader(USER_TYPE_HEADER)),
                parseRole(request.getHeader(ROLE_HEADER))
        );

        try {
            Optional<Member> memberOptional = memberService.findBySso(provisionalPrincipal);
            if (memberOptional.isEmpty()) {
                throw new BusinessException(MemberErrorCode.REGISTRATION_REQUIRED);
            }

            Member member = memberOptional.get();
            if (member.isBanned()) {
                throw new BusinessException(MemberErrorCode.MEMBER_BANNED);
            }
            if (member.isSuspended()) {
                throw new BusinessException(MemberErrorCode.MEMBER_SUSPENDED);
            }
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
}
