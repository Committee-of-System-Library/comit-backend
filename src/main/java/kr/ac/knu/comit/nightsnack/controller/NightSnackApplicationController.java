package kr.ac.knu.comit.nightsnack.controller;

import java.time.LocalDate;
import kr.ac.knu.comit.nightsnack.controller.api.NightSnackApplicationControllerApi;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.dto.MyTicketResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
import kr.ac.knu.comit.nightsnack.service.NightSnackQueryService;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NightSnackApplicationController implements NightSnackApplicationControllerApi {

    private final NightSnackApplicationService nightSnackApplicationService;
    private final NightSnackQueryService nightSnackQueryService;

    @Override
    public ResponseEntity<ApiResponse<NightSnackResponse>> getNightSnack(LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(nightSnackQueryService.getByDate(date)));
    }

    @Override
    public ResponseEntity<ApiResponse<MyTicketResponse>> getMyTicket(Long nightSnackId, MemberPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                nightSnackQueryService.getMyTicket(nightSnackId, principal.memberId())));
    }

    @Override
    public ResponseEntity<ApiResponse<ApplyResponse>> apply(Long nightSnackId, MemberPrincipal principal) {
        ApplyResponse response = nightSnackApplicationService.apply(nightSnackId, principal.memberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
