package kr.ac.knu.comit.attachment.controller;

import kr.ac.knu.comit.attachment.controller.api.AttachmentControllerApi;
import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadRequest;
import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadResponse;
import kr.ac.knu.comit.attachment.service.AttachmentService;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AttachmentController implements AttachmentControllerApi {

    private final AttachmentService attachmentService;

    @Override
    public ResponseEntity<ApiResponse<PresignedPdfUploadResponse>> generatePresignedUploadUrl(
            PresignedPdfUploadRequest request,
            MemberPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.generatePresignedUploadUrl(request)));
    }
}
