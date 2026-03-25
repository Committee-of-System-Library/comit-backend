package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "/problems/common/internal-server-error", "서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "/problems/common/invalid-request", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "/problems/common/unauthorized", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "/problems/common/forbidden", "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String type;
    private final String message;

    CommonErrorCode(HttpStatus status, String type, String message) {
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
