package kr.ac.knu.comit.auth.dto;

public record SsoCallbackRejected(
        String redirectUrl,
        String clearStateCookieHeader
) implements SsoCallbackResult {
}
