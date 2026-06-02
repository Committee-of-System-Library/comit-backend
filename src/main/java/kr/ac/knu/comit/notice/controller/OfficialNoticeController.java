package kr.ac.knu.comit.notice.controller;

import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.notice.controller.api.OfficialNoticeControllerApi;
import kr.ac.knu.comit.notice.dto.OfficialNoticeListResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeSearchResponse;
import kr.ac.knu.comit.notice.service.OfficialNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OfficialNoticeController implements OfficialNoticeControllerApi {

    private final OfficialNoticeService officialNoticeService;

    @Override
    public ResponseEntity<ApiResponse<OfficialNoticeSearchResponse>> searchNotices(String query, int size) {
        return ResponseEntity.ok(ApiResponse.success(
                officialNoticeService.searchNotices(query, size)));
    }

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
}
