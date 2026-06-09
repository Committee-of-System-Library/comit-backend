package kr.ac.knu.comit.inquiry.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.inquiry.controller.api.AdminInquiryControllerApi;
import kr.ac.knu.comit.inquiry.dto.AdminInquiryPageResponse;
import kr.ac.knu.comit.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminInquiryController implements AdminInquiryControllerApi {

    private final InquiryService inquiryService;

    @Override
    public ResponseEntity<ApiResponse<AdminInquiryPageResponse>> getInquiries(Pageable pageable, MemberPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getInquiries(pageable)));
    }
}
