package kr.ac.knu.comit.attachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadRequest;
import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.StorageErrorCode;
import kr.ac.knu.comit.global.storage.S3StorageUploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentService")
class AttachmentServiceTest {

    @Mock
    private S3StorageUploader s3StorageUploader;

    @InjectMocks
    private AttachmentService attachmentService;

    @Nested
    @DisplayName("generatePresignedUploadUrl")
    class GeneratePresignedUploadUrl {

        @Test
        @DisplayName("application/pdf + 허용 폴더면 S3 presigned URL을 발급한다")
        void returnsPresignedUrl_whenValidRequest() {
            PresignedPdfUploadRequest request = new PresignedPdfUploadRequest(
                    "portfolio.pdf", "application/pdf", "portfolio"
            );
            given(s3StorageUploader.generatePresignedUploadUrl("portfolio", "portfolio.pdf", "application/pdf"))
                    .willReturn(new S3StorageUploader.PresignedUploadUrls(
                            "https://bucket.s3/portfolio/uuid.pdf?sig=...",
                            "https://bucket.s3/portfolio/uuid.pdf"
                    ));

            PresignedPdfUploadResponse response = attachmentService.generatePresignedUploadUrl(request);

            assertThat(response.presignedUrl()).isEqualTo("https://bucket.s3/portfolio/uuid.pdf?sig=...");
            assertThat(response.fileUrl()).isEqualTo("https://bucket.s3/portfolio/uuid.pdf");
        }

        @Test
        @DisplayName("application/pdf 외 contentType이면 UNSUPPORTED_FILE_TYPE 예외")
        void throwsUnsupportedFileType_whenNonPdfContentType() {
            PresignedPdfUploadRequest request = new PresignedPdfUploadRequest(
                    "image.png", "image/png", "portfolio"
            );

            assertThatThrownBy(() -> attachmentService.generatePresignedUploadUrl(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(StorageErrorCode.UNSUPPORTED_FILE_TYPE);
            then(s3StorageUploader).should(never()).generatePresignedUploadUrl(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString()
            );
        }

        @Test
        @DisplayName("화이트리스트에 없는 folder면 INVALID_REQUEST 예외")
        void throwsInvalidRequest_whenFolderNotAllowed() {
            PresignedPdfUploadRequest request = new PresignedPdfUploadRequest(
                    "doc.pdf", "application/pdf", "secret"
            );

            assertThatThrownBy(() -> attachmentService.generatePresignedUploadUrl(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }
}