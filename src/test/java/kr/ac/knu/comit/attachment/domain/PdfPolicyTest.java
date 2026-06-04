package kr.ac.knu.comit.attachment.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.StorageErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PdfPolicy")
class PdfPolicyTest {

    @Nested
    @DisplayName("requireContentType")
    class RequireContentType {

        @Test
        @DisplayName("application/pdf는 통과")
        void passesForPdf() {
            assertThatCode(() -> PdfPolicy.requireContentType("application/pdf")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("그 외 contentType은 UNSUPPORTED_FILE_TYPE")
        void throwsForOther() {
            assertThatThrownBy(() -> PdfPolicy.requireContentType("image/png"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(StorageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    @Nested
    @DisplayName("requireSize")
    class RequireSize {

        @Test
        @DisplayName("20MB 이하는 통과")
        void passesUnderLimit() {
            assertThatCode(() -> PdfPolicy.requireSize(PdfPolicy.MAX_FILE_SIZE)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("20MB 초과는 FILE_SIZE_EXCEEDED")
        void throwsOverLimit() {
            assertThatThrownBy(() -> PdfPolicy.requireSize(PdfPolicy.MAX_FILE_SIZE + 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(StorageErrorCode.FILE_SIZE_EXCEEDED);
        }
    }
}
