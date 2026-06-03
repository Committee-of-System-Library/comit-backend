package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum NoticeErrorCode implements ErrorCode {
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "/problems/notice/not-found", "존재하지 않는 공지사항입니다."),
    INVALID_NOTICE_TITLE(HttpStatus.BAD_REQUEST, "/problems/notice/invalid-title", "제목은 1~300자이어야 합니다."),
    INVALID_NOTICE_CONTENT(HttpStatus.BAD_REQUEST, "/problems/notice/invalid-content", "내용을 입력해주세요."),
    CHAT_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "/problems/notice/chat-timeout", "공지 챗봇 응답 시간이 초과되었습니다."),
    CHAT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "/problems/notice/chat-unavailable", "챗봇이 일시적으로 과부하 상태입니다. 잠시 후 다시 시도해주세요.");

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
