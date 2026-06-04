package kr.ac.knu.comit.attachment.domain;

import java.util.Arrays;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;

public enum AttachmentFolder {
    PORTFOLIO("portfolio"),
    REVIEW("review");

    private final String value;

    AttachmentFolder(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AttachmentFolder from(String raw) {
        return Arrays.stream(values())
                .filter(folder -> folder.value.equals(raw))
                .findFirst()
                .orElseThrow(() -> new BusinessException("허용되지 않은 폴더입니다.", CommonErrorCode.INVALID_REQUEST));
    }
}
