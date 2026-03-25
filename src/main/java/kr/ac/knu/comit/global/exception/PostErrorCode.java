package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum PostErrorCode implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "/problems/post/not-found", "존재하지 않는 게시글입니다."),
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "/problems/post/invalid-title", "제목은 1~30자이어야 합니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "/problems/post/invalid-content", "내용은 1~500자이어야 합니다."),
    TAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "/problems/post/tag-limit-exceeded", "태그는 최대 5개까지 입력할 수 있습니다."),
    INVALID_TAG(HttpStatus.BAD_REQUEST, "/problems/post/invalid-tag", "태그는 1~20자이어야 합니다.");

    private final HttpStatus status;
    private final String type;
    private final String message;

    PostErrorCode(HttpStatus status, String type, String message) {
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
