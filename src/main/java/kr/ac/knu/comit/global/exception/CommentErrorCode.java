package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "/problems/comment/not-found", "존재하지 않는 댓글입니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "/problems/comment/invalid-parent", "유효한 부모 댓글이 아닙니다."),
    INVALID_COMMENT_CONTENT(HttpStatus.BAD_REQUEST, "/problems/comment/invalid-content", "댓글 내용을 입력해주세요.");

    private final HttpStatus status;
    private final String type;
    private final String message;

    CommentErrorCode(HttpStatus status, String type, String message) {
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
