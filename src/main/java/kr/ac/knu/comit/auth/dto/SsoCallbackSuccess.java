package kr.ac.knu.comit.auth.dto;

public record SsoCallbackSuccess(
        String redirectUrl,
        String tokenCookieHeader,
        String clearStateCookieHeader
) {
}
