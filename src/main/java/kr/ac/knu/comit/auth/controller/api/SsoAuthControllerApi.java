package kr.ac.knu.comit.auth.controller.api;

import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
@RequestMapping("/auth/sso")
public interface SsoAuthControllerApi {

    @ApiDoc(
            summary = "SSO 로그인 시작",
            description = "auth-server 로그인 엔드포인트로 리다이렉트하고 CSRF 방지를 위한 state 쿠키를 발급합니다.",
            descriptions = {
                    @FieldDesc(name = "stateCookie", value = "callback state 검증에 사용할 HttpOnly 쿠키입니다."),
                    @FieldDesc(name = "Location", value = "auth-server 로그인 페이지로 이동할 redirect URL입니다.")
            }
    )
    @GetMapping("/login")
    ResponseEntity<Void> startLogin();

    @ApiDoc(
            summary = "SSO 로그인 콜백 처리",
            description = "auth-server가 전달한 custom JWT와 state를 검증한 뒤, SSO 토큰 쿠키를 심고 프론트 성공 URL로 리다이렉트합니다.",
            descriptions = {
                    @FieldDesc(name = "state", value = "로그인 시작 시 발급한 state와 일치해야 합니다."),
                    @FieldDesc(name = "token", value = "auth-server가 내려준 하위 서비스용 custom JWT입니다."),
                    @FieldDesc(name = "tokenCookie", value = "이후 API 인증에 사용할 HttpOnly 쿠키입니다.")
            },
            errors = {
                    @ApiError(code = "INVALID_REQUEST", when = "state 값이 누락되거나 기존 state cookie와 일치하지 않을 때"),
                    @ApiError(code = "UNAUTHORIZED", when = "token 서명, issuer, audience, 만료 시간이 유효하지 않을 때")
            }
    )
    @GetMapping("/callback")
    ResponseEntity<Void> handleCallback(
            @RequestParam String state,
            @RequestParam String token,
            HttpServletRequest request
    );

    @ApiDoc(
            summary = "SSO 로그아웃",
            description = "Comit이 보관하던 SSO 토큰 쿠키와 state 쿠키를 제거합니다."
    )
    @PostMapping("/logout")
    ResponseEntity<Void> logout();
}
