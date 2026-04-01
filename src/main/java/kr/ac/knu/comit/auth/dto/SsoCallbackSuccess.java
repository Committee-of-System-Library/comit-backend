package kr.ac.knu.comit.auth.dto;

public record SsoCallbackSuccess(
        String redirectUrl,
        String tokenCookieHeader,
        String clearStateCookieHeader,
        String clearRedirectUriCookieHeader
) implements SsoCallbackResult {

    public SsoCallbackSuccess(String redirectUrl, String tokenCookieHeader, String clearStateCookieHeader) {
        this(redirectUrl, tokenCookieHeader, clearStateCookieHeader, null);
    }
}
