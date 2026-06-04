package kr.ac.knu.comit.attachment.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadRequest;
import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadResponse;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/attachments/pdf")
public interface AttachmentControllerApi {

    @ApiDoc(
            summary = "PDF 첨부 Presigned 업로드 URL 발급",
            description = "PDF 파일을 S3에 직접 업로드할 수 있는 presigned URL을 발급합니다. 발급된 presignedUrl로 PUT 요청을 보내 업로드하고, fileUrl을 후속 메타 저장 API(포트폴리오, 리뷰 게시글)에 사용하세요. URL 유효시간은 10분이며 contentType은 application/pdf만 허용됩니다.",
            descriptions = {
                    @FieldDesc(name = "fileName", value = "업로드할 파일명 (.pdf 확장자 포함)"),
                    @FieldDesc(name = "contentType", value = "application/pdf 고정"),
                    @FieldDesc(name = "folder", value = "저장할 폴더명 (portfolio | review)"),
                    @FieldDesc(name = "presignedUrl", value = "S3에 직접 PUT 업로드할 서명된 URL (10분 유효)"),
                    @FieldDesc(name = "fileUrl", value = "업로드 완료 후 메타 저장에 사용할 public URL")
            },
            errors = {
                    @ApiError(code = "UNSUPPORTED_FILE_TYPE", when = "application/pdf 외 contentType을 요청했을 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "허용되지 않은 folder(portfolio/review 외)를 요청했을 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "presignedUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/portfolio/550e8400.pdf?X-Amz-Signature=...",
                                "fileUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/portfolio/550e8400.pdf"
                              }
                            }
                            """
            )
    )
    @PostMapping("/presigned-upload")
    ResponseEntity<ApiResponse<PresignedPdfUploadResponse>> generatePresignedUploadUrl(
            @Valid @RequestBody PresignedPdfUploadRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );
}
