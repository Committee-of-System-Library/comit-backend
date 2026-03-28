package kr.ac.knu.comit.auth.controller;

import kr.ac.knu.comit.auth.controller.api.SsoAuthControllerApi;
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
        SsoCallbackSuccess callbackSuccess = ssoAuthService.handleCallback(state, token, authCookieManager.resolveStateCookie(request));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, callbackSuccess.tokenCookieHeader())
                .header(HttpHeaders.SET_COOKIE, callbackSuccess.clearStateCookieHeader())
                .header(HttpHeaders.LOCATION, callbackSuccess.redirectUrl())
                .build();
    }

    @Override
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, ssoAuthService.clearAuthenticationCookie())
                .build();
    }
}
