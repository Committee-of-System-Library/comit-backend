package kr.ac.knu.comit.auth.dto;

public record SsoCallbackPendingRegistration(
        String redirectUrl,
        String tokenCookie,
        String clearStateCookie,
        String clearRedirectUriCookieHeader
) implements SsoCallbackResult {

    public SsoCallbackPendingRegistration(String redirectUrl, String tokenCookie, String clearStateCookie) {
        this(redirectUrl, tokenCookie, clearStateCookie, null);
    }
}
