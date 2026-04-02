package kr.ac.knu.comit.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@ConditionalOnProperty("comit.dev.auth.enabled")
@RestController
@RequestMapping("/auth/dev")
@RequiredArgsConstructor
public class DevAuthController {

    public static final String DEV_AUTH_COOKIE = "comit-dev-auth";

    private final MemberRepository memberRepository;
    private final ComitSsoProperties ssoProperties;

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestParam String nickname,
            @RequestParam(defaultValue = "STUDENT") String role,
            HttpServletResponse response
    ) {
        Member member = memberRepository.findByNicknameAndDeletedAtIsNull(nickname)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        String cookieValue = member.getSsoSub() + "|" + role.toUpperCase();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(cookieValue, Duration.ofDays(7)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO));
        return ResponseEntity.ok().build();
    }

    private String buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(DEV_AUTH_COOKIE, value)
                .httpOnly(true)
                .secure(ssoProperties.isCookieSecure())
                .sameSite(ssoProperties.getCookieSameSite())
                .path(cookiePath())
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private String cookiePath() {
        String path = ssoProperties.getCookiePath();
        return (path == null || path.isBlank()) ? "/" : path;
    }
}
