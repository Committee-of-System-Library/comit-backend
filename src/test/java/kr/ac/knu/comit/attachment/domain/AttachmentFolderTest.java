package kr.ac.knu.comit.attachment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AttachmentFolder")
class AttachmentFolderTest {

    @Test
    @DisplayName("portfolio raw → PORTFOLIO")
    void parsesPortfolio() {
        assertThat(AttachmentFolder.from("portfolio")).isEqualTo(AttachmentFolder.PORTFOLIO);
    }

    @Test
    @DisplayName("review raw → REVIEW")
    void parsesReview() {
        assertThat(AttachmentFolder.from("review")).isEqualTo(AttachmentFolder.REVIEW);
    }

    @Test
    @DisplayName("화이트리스트에 없는 값이면 INVALID_REQUEST")
    void throwsOnUnknownFolder() {
        assertThatThrownBy(() -> AttachmentFolder.from("secret"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }
}
