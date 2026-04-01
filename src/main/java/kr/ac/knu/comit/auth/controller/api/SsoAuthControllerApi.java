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
            description = "auth-server 로그인 엔드포인트로 리다이렉트하고 CSRF 방지를 위한 state 쿠키를 발급합니다. 선택적 redirectUri가 있으면 allowlist 검증 후 동적 복귀 URL로 사용합니다.",
            descriptions = {
                    @FieldDesc(name = "stateCookie", value = "callback state 검증에 사용할 HttpOnly 쿠키입니다."),
                    @FieldDesc(name = "redirectUri", value = "선택적 복귀 base URL입니다. allowlist origin exact match 검증 후 저장됩니다."),
                    @FieldDesc(name = "Location", value = "auth-server 로그인 페이지로 이동할 redirect URL입니다.")
            },
            errors = {
                    @ApiError(code = "INVALID_REQUEST", when = "redirectUri가 절대 URI가 아니거나 allowlist origin 검증에 실패할 때")
            }
    )
    @GetMapping("/login")
    ResponseEntity<Void> startLogin(
            @RequestParam(required = false) String redirectUri
    );

    @ApiDoc(
            summary = "SSO 로그인 콜백 처리",
            description = "auth-server가 전달한 custom JWT와 state를 검증한 뒤, 가입 상태에 따라 stage=success|register|error를 붙여 프론트로 리다이렉트합니다. 로그인 시작 시 저장된 redirectUri cookie가 있으면 그 URL을 우선 사용하고, 콜백 완료 후 redirectUri cookie를 제거합니다.",
            descriptions = {
                    @FieldDesc(name = "state", value = "로그인 시작 시 발급한 state와 일치해야 합니다."),
                    @FieldDesc(name = "token", value = "auth-server가 내려준 하위 서비스용 custom JWT입니다."),
                    @FieldDesc(name = "tokenCookie", value = "이후 API 인증과 회원가입 진행에 사용할 HttpOnly 쿠키입니다."),
                    @FieldDesc(name = "redirectUriCookie", value = "로그인 시작 시 저장된 복귀 base URL 쿠키입니다. 콜백 완료 후 제거됩니다."),
                    @FieldDesc(name = "Location", value = "기가입 사용자는 stage=success, 미가입 사용자는 stage=register, 외부 사용자는 stage=error&reason=... URL로 이동합니다.")
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
