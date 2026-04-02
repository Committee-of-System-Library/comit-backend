package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum StorageErrorCode implements ErrorCode {
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "/problems/storage/file-upload-failed", "파일 업로드에 실패했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "/problems/storage/file-size-exceeded", "파일 크기는 5MB를 초과할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "/problems/storage/unsupported-file-type", "지원하지 않는 파일 형식입니다. (허용: jpg, jpeg, png, webp, gif)");

    private final HttpStatus status;
    private final String type;
    private final String message;

    StorageErrorCode(HttpStatus status, String type, String message) {
        this.status = status;
        this.type = type;
        this.message = message;
    }

    @Override
    public int getStatus() {
        return status.value();
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return type;
    }
}
