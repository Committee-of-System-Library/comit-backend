package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum ReportErrorCode implements ErrorCode {
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "/problems/report/already-exists", "이미 신고한 대상입니다.");

    private final HttpStatus status;
    private final String type;
    private final String message;

    ReportErrorCode(HttpStatus status, String type, String message) {
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
