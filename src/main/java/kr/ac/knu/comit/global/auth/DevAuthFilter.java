package kr.ac.knu.comit.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ac.knu.comit.auth.controller.DevAuthController;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@ConditionalOnProperty("comit.dev.auth.enabled")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DevAuthFilter extends OncePerRequestFilter {

    private final MemberRepository memberRepository;
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

        String cookieValue = resolveCookie(request);
        if (cookieValue == null) {
            log.debug("[DevAuth] no cookie → anonymous, uri={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String[] parts = cookieValue.split("\\|", 2);
            String ssoSub = parts[0];

            memberRepository.findBySsoSubAndDeletedAtIsNull(ssoSub).ifPresentOrElse(member -> {
                MemberPrincipal.MemberRole memberRole = member.getComitRole() == kr.ac.knu.comit.member.domain.ComitRole.ADMIN
                        ? MemberPrincipal.MemberRole.ADMIN
                        : MemberPrincipal.MemberRole.STUDENT;
                log.debug("[DevAuth] ssoSub={} comitRole={} → memberRole={} uri={}",
                        ssoSub, member.getComitRole(), memberRole, request.getRequestURI());
                MemberPrincipal principal = new MemberPrincipal(
                        member.getId(),
                        member.getSsoSub(),
                        member.getName(),
                        null,
                        member.getStudentNumber(),
                        MemberPrincipal.UserType.CSE_STUDENT,
                        memberRole
                );
                request.setAttribute(MemberArgumentResolver.PRINCIPAL_ATTRIBUTE, principal);
            }, () -> log.debug("[DevAuth] ssoSub={} not found in DB → anonymous", ssoSub));
        } catch (Exception e) {
            // 쿠키가 오염됐거나 DB 재시드 후 stale 상태인 경우 anonymous로 fallthrough
            log.warn("[DevAuth] cookie parse failed → anonymous, error={}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return isAuthPath(servletPath, "/auth/dev")
                || isAuthPath(servletPath, "/auth/sso")
                || isAuthPath(servletPath, "/auth/register");
    }

    private boolean isAuthPath(String servletPath, String path) {
        return servletPath.equals(path) || servletPath.startsWith(path + "/");
    }

    private String resolveCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (DevAuthController.DEV_AUTH_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
