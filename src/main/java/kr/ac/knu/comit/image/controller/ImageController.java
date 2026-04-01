package kr.ac.knu.comit.image.controller;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.image.controller.api.ImageControllerApi;
import kr.ac.knu.comit.image.dto.UploadImageResponse;
import kr.ac.knu.comit.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ImageController implements ImageControllerApi {

    private final ImageService imageService;

    @Override
    public ResponseEntity<ApiResponse<UploadImageResponse>> uploadImage(
            MultipartFile file,
            String folder,
            MemberPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(imageService.upload(file, folder)));
    }
}
