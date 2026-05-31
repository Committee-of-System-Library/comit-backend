package kr.ac.knu.comit.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knu.comit.auth.controller.api.RegisterControllerApi;
import kr.ac.knu.comit.auth.dto.RegisterPrefillResponse;
import kr.ac.knu.comit.auth.dto.RegisterProfileImagePresignedRequest;
import kr.ac.knu.comit.auth.dto.RegisterRequest;
import kr.ac.knu.comit.auth.service.AuthCookieManager;
import kr.ac.knu.comit.auth.service.RegisterService;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.image.dto.PresignedUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegisterController implements RegisterControllerApi {

    private final RegisterService registerService;
    private final AuthCookieManager authCookieManager;

    @Override
    public ResponseEntity<ApiResponse<RegisterPrefillResponse>> getPrefill(HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(registerService.getPrefill(authCookieManager.resolveTokenCookie(request)))
        );
    }

    @Override
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> createProfileImagePresignedUpload(
            RegisterProfileImagePresignedRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(registerService.createProfileImagePresignedUpload(
                        authCookieManager.resolveTokenCookie(httpServletRequest),
                        request
                ))
        );
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> register(RegisterRequest request, HttpServletRequest httpServletRequest) {
        registerService.register(authCookieManager.resolveTokenCookie(httpServletRequest), request);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
