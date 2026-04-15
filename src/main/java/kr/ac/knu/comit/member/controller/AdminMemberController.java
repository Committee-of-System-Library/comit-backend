package kr.ac.knu.comit.member.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.controller.api.AdminMemberControllerApi;
import kr.ac.knu.comit.member.domain.MemberStatus;
import kr.ac.knu.comit.member.dto.AdminMemberPageResponse;
import kr.ac.knu.comit.member.dto.AdminMemberStatusRequest;
import kr.ac.knu.comit.member.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminMemberController implements AdminMemberControllerApi {

    private final AdminMemberService adminMemberService;

    @Override
    public ResponseEntity<ApiResponse<AdminMemberPageResponse>> getMembers(
            MemberStatus status, Pageable pageable, MemberPrincipal principal) {
        log.debug("Admin API called - principal: {}, role: {}, isAdmin: {}",
            principal.memberId(), principal.role(), principal.isAdmin());
        validateAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberService.getMembers(status, pageable)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updateMemberStatus(
            Long memberId, AdminMemberStatusRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        adminMemberService.updateMemberStatus(memberId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            Long memberId, MemberPrincipal principal) {
        validateAdmin(principal);
        adminMemberService.deleteMember(memberId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateAdmin(MemberPrincipal principal) {
        if (!principal.isAdmin()) {
            log.warn("Admin access denied - memberId: {}, role: {}",
                principal.memberId(), principal.role());
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
