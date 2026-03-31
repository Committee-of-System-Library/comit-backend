package kr.ac.knu.comit.auth.controller;

import kr.ac.knu.comit.auth.controller.api.SsoAuthControllerApi;
import kr.ac.knu.comit.auth.dto.SsoCallbackPendingRegistration;
import kr.ac.knu.comit.auth.dto.SsoCallbackRejected;
import kr.ac.knu.comit.auth.dto.SsoCallbackResult;
import kr.ac.knu.comit.auth.dto.SsoCallbackSuccess;
import kr.ac.knu.comit.auth.dto.SsoLoginStart;
import kr.ac.knu.comit.auth.service.AuthCookieManager;
import kr.ac.knu.comit.auth.service.SsoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
public class SsoAuthController implements SsoAuthControllerApi {

    private final SsoAuthService ssoAuthService;
    private final AuthCookieManager authCookieManager;

    @Override
    public ResponseEntity<Void> startLogin() {
        SsoLoginStart loginStart = ssoAuthService.startLogin();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, loginStart.stateCookieHeader())
                .header(HttpHeaders.LOCATION, loginStart.loginUrl())
                .build();
    }

    @Override
    public ResponseEntity<Void> handleCallback(String state, String token, HttpServletRequest request) {
        SsoCallbackResult result = ssoAuthService.handleCallback(state, token, authCookieManager.resolveStateCookie(request));
        return switch (result) {
            case SsoCallbackSuccess success -> ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, success.tokenCookieHeader())
                    .header(HttpHeaders.SET_COOKIE, success.clearStateCookieHeader())
                    .header(HttpHeaders.LOCATION, success.redirectUrl())
                    .build();
            case SsoCallbackPendingRegistration pendingRegistration -> ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, pendingRegistration.tokenCookie())
                    .header(HttpHeaders.SET_COOKIE, pendingRegistration.clearStateCookie())
                    .header(HttpHeaders.LOCATION, pendingRegistration.redirectUrl())
                    .build();
            case SsoCallbackRejected rejected -> ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, rejected.clearStateCookieHeader())
                    .header(HttpHeaders.LOCATION, rejected.redirectUrl())
                    .build();
        };
    }

    @Override
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, ssoAuthService.clearAuthenticationCookie())
                .build();
    }
}
