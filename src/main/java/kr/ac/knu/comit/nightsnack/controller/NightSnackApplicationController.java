package kr.ac.knu.comit.nightsnack.controller;

import kr.ac.knu.comit.nightsnack.controller.api.NightSnackApplicationControllerApi;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NightSnackApplicationController implements NightSnackApplicationControllerApi {

    private final NightSnackApplicationService nightSnackApplicationService;

    @Override
    public ResponseEntity<ApiResponse<ApplyResponse>> apply(Long nightSnackId, MemberPrincipal principal) {
        ApplyResponse response = nightSnackApplicationService.apply(nightSnackId, principal.memberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
