package kr.ac.knu.comit.attachment.domain;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.StorageErrorCode;

public final class PdfPolicy {

    public static final String CONTENT_TYPE = "application/pdf";
    public static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private PdfPolicy() {
    }

    public static void requireContentType(String contentType) {
        if (!CONTENT_TYPE.equals(contentType)) {
            throw new BusinessException("PDF만 업로드 가능합니다.", StorageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    public static void requireSize(long size) {
        if (size > MAX_FILE_SIZE) {
            throw new BusinessException("PDF는 20MB를 초과할 수 없습니다.", StorageErrorCode.FILE_SIZE_EXCEEDED);
        }
    }
}