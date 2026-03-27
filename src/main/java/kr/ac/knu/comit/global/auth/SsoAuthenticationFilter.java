package kr.ac.knu.comit.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.dto.SsoClaims;
import kr.ac.knu.comit.auth.service.SsoTokenVerifier;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "comit.auth.sso", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SsoAuthenticationFilter extends OncePerRequestFilter {

    private final ComitSsoProperties ssoProperties;
    private final SsoTokenVerifier ssoTokenVerifier;
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

        String token = readTokenCookie(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SsoClaims claims = ssoTokenVerifier.verify(token);
        MemberPrincipal provisionalPrincipal = new MemberPrincipal(
                null,
                claims.subject(),
                claims.name(),
                claims.email(),
                claims.studentNumber(),
                claims.userType(),
                claims.role()
        );

        Member member = memberService.findOrCreateBySso(provisionalPrincipal);
        request.setAttribute(
                MemberArgumentResolver.PRINCIPAL_ATTRIBUTE,
                new MemberPrincipal(
                        member.getId(),
                        member.getSsoSub(),
                        provisionalPrincipal.name(),
                        provisionalPrincipal.email(),
                        provisionalPrincipal.studentNumber(),
                        provisionalPrincipal.userType(),
                        provisionalPrincipal.role()
                )
        );

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/auth/sso");
    }

    private String readTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ssoProperties.getTokenCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
