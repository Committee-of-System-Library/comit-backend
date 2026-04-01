package kr.ac.knu.comit.auth.dto;

public record SsoLoginStart(
        String loginUrl,
        String stateCookieHeader,
        String redirectUriCookieHeader
) {

    public SsoLoginStart(String loginUrl, String stateCookieHeader) {
        this(loginUrl, stateCookieHeader, null);
    }
}
