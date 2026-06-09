package kr.ac.knu.comit.inquiry.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.inquiry.controller.api.InquiryControllerApi;
import kr.ac.knu.comit.inquiry.dto.CreateInquiryRequest;
import kr.ac.knu.comit.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InquiryController implements InquiryControllerApi {

    private final InquiryService inquiryService;

    @Override
    public ResponseEntity<ApiResponse<Void>> create(CreateInquiryRequest request, MemberPrincipal principal) {
        inquiryService.create(principal.memberId(), request);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
