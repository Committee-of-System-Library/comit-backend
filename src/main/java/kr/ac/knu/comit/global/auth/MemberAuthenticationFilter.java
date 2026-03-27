package kr.ac.knu.comit.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

        Member member = memberService.findOrCreateBySso(provisionalPrincipal);
        MemberPrincipal authenticatedPrincipal = new MemberPrincipal(
                member.getId(),
                member.getSsoSub(),
                provisionalPrincipal.name(),
                provisionalPrincipal.email(),
                provisionalPrincipal.studentNumber(),
                provisionalPrincipal.userType(),
                provisionalPrincipal.role()
        );

        request.setAttribute(MemberArgumentResolver.PRINCIPAL_ATTRIBUTE, authenticatedPrincipal);
        filterChain.doFilter(request, response);
    }

    private String defaultName(HttpServletRequest request) {
        String name = trimToNull(request.getHeader(NAME_HEADER));
        return name != null ? name : "comit-user";
    }

    private MemberPrincipal.UserType parseUserType(String rawValue) {
        return parseEnum(rawValue, MemberPrincipal.UserType.CSE_STUDENT, MemberPrincipal.UserType.class);
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
