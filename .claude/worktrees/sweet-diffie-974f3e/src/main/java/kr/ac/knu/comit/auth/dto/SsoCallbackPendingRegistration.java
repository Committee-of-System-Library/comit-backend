package kr.ac.knu.comit.auth.dto;

public record SsoCallbackPendingRegistration(
        String redirectUrl,
        String tokenCookie,
        String clearStateCookie,
        String clearRedirectUriCookieHeader
) implements SsoCallbackResult {
}
