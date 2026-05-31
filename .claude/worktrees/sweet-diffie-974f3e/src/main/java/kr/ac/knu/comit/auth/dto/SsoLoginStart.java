package kr.ac.knu.comit.auth.dto;

public record SsoLoginStart(
        String loginUrl,
        String stateCookieHeader,
        String redirectUriCookieHeader
) {
}
