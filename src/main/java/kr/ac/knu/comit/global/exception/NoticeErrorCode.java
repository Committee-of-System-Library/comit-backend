package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum NoticeErrorCode implements ErrorCode {
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "/problems/notice/not-found", "존재하지 않는 공지사항입니다."),
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "/problems/notice/invalid-title", "제목은 1~300자이어야 합니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "/problems/notice/invalid-content", "내용을 입력해주세요.");

    private final HttpStatus status;
    private final String type;
    private final String message;

    NoticeErrorCode(HttpStatus status, String type, String message) {
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
