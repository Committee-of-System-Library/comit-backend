package kr.ac.knu.comit.nightsnack.controller;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knu.comit.nightsnack.controller.api.AdminNightSnackControllerApi;
import kr.ac.knu.comit.nightsnack.dto.ApplicationCheckResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.CreateNightSnackRequest;
import kr.ac.knu.comit.nightsnack.dto.CreateNightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.ReserveRequest;
import kr.ac.knu.comit.nightsnack.dto.ReserveResponse;
import kr.ac.knu.comit.nightsnack.service.AdminNightSnackService;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminNightSnackController implements AdminNightSnackControllerApi {

    private final AdminNightSnackService adminNightSnackService;

    @Override
    public ResponseEntity<ApiResponse<CreateNightSnackResponse>> createNightSnack(
            CreateNightSnackRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        Long nightSnackId = adminNightSnackService.createNightSnack(
                request.nightSnackDate(), request.capacity(), request.toPeriod(),
                request.title(), request.contents(), request.menu(),
                request.pickupLocation(), request.pickupStartTime(), request.pickupDeadline(), request.requiresStudentCouncilFee());
        return ResponseEntity.ok(ApiResponse.success(CreateNightSnackResponse.from(nightSnackId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> openNightSnack(Long nightSnackId, MemberPrincipal principal) {
        validateAdmin(principal);
        adminNightSnackService.open(nightSnackId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<ReserveResponse>> reserve(
            LocalDate date, ReserveRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        ReserveResponse response = adminNightSnackService.reserve(date, request.studentNumbers());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<List<NightSnackResponse>>> listNightSnacks(MemberPrincipal principal) {
        validateAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(adminNightSnackService.listNightSnacks()));
    }

    @Override
    public ResponseEntity<ApiResponse<ApplicationCheckResponse>> checkApplication(
            Long nightSnackId, String studentNumber, MemberPrincipal principal) {
        validateAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(adminNightSnackService.checkApplication(nightSnackId, studentNumber)));
    }

    private void validateAdmin(MemberPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
