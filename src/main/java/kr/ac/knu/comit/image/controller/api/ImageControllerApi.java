package kr.ac.knu.comit.image.controller.api;

import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.image.dto.UploadImageResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@ApiContract
@RequestMapping("/images")
public interface ImageControllerApi {

    @ApiDoc(
            summary = "이미지 업로드",
            description = "이미지를 지정한 폴더에 업로드하고 public URL을 반환합니다. folder 값으로 저장 경로를 구분합니다. (예: members, posts)",
            descriptions = {
                    @FieldDesc(name = "file", value = "업로드할 이미지 파일"),
                    @FieldDesc(name = "folder", value = "저장할 폴더명 (예: members, posts)"),
                    @FieldDesc(name = "url", value = "업로드된 이미지의 public URL")
            },
            errors = {
                    @ApiError(code = "FILE_SIZE_EXCEEDED", when = "파일 크기가 5MB를 초과했을 때"),
                    @ApiError(code = "UNSUPPORTED_FILE_TYPE", when = "jpg, jpeg, png, webp, gif 외 파일을 업로드할 때"),
                    @ApiError(code = "FILE_UPLOAD_FAILED", when = "S3 업로드 중 오류가 발생했을 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "url": "https://bucket.s3.ap-northeast-2.amazonaws.com/posts/550e8400-e29b-41d4-a716-446655440000.png"
                              }
                            }
                            """
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<UploadImageResponse>> uploadImage(
            @RequestPart MultipartFile file,
            @RequestParam String folder,
            @AuthenticatedMember MemberPrincipal principal
    );
}
