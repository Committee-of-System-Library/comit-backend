package kr.ac.knu.comit.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.auth.service.AuthCookieManager;
import kr.ac.knu.comit.auth.service.ExternalIdentityMapper;
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

    private final AuthCookieManager authCookieManager;
    private final ExternalAuthClient externalAuthClient;
    private final ExternalIdentityMapper externalIdentityMapper;
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

        String token = authCookieManager.resolveTokenCookie(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ExternalIdentity identity = externalAuthClient.verify(token);
            MemberPrincipal provisionalPrincipal = externalIdentityMapper.toPrincipal(identity);

            Member member = memberService.findOrCreateBySso(provisionalPrincipal);
            request.setAttribute(
                    MemberArgumentResolver.PRINCIPAL_ATTRIBUTE,
                    externalIdentityMapper.toPrincipal(member.getId(), identity)
            );
        } catch (Exception exception) {
            response.addHeader(HttpHeaders.SET_COOKIE, authCookieManager.clearAuthenticationCookie());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/auth/sso");
    }
}
