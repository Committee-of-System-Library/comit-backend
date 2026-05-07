package kr.ac.knu.comit.notice.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.notice.controller.api.OfficialNoticeControllerApi;
import kr.ac.knu.comit.notice.dto.CreateOfficialNoticeRequest;
import kr.ac.knu.comit.notice.dto.OfficialNoticeListResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeResponse;
import kr.ac.knu.comit.notice.dto.UpdateOfficialNoticeRequest;
import kr.ac.knu.comit.notice.service.OfficialNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OfficialNoticeController implements OfficialNoticeControllerApi {

    private final OfficialNoticeService officialNoticeService;

    @Override
    public ResponseEntity<ApiResponse<OfficialNoticeListResponse>> getNotices(Long cursor, int size) {
        return ResponseEntity.ok(ApiResponse.success(
                officialNoticeService.getNotices(cursor, size)));
    }

    @Override
    public ResponseEntity<ApiResponse<OfficialNoticeResponse>> getNotice(Long noticeId) {
        return ResponseEntity.ok(ApiResponse.success(
                officialNoticeService.getNotice(noticeId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Long>> createNotice(
            CreateOfficialNoticeRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        Long noticeId = officialNoticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(noticeId));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updateNotice(
            Long noticeId, UpdateOfficialNoticeRequest request, MemberPrincipal principal) {
        validateAdmin(principal);
        officialNoticeService.updateNotice(noticeId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteNotice(Long noticeId, MemberPrincipal principal) {
        validateAdmin(principal);
        officialNoticeService.deleteNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateAdmin(MemberPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
