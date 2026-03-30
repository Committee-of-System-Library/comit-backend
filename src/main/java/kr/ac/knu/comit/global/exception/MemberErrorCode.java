package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "/problems/member/not-found", "존재하지 않는 회원입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "/problems/member/duplicate-nickname", "이미 사용 중인 닉네임입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "/problems/member/invalid-nickname", "닉네임은 1~15자이어야 합니다."),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "/problems/member/suspended", "정지된 계정입니다."),
    MEMBER_BANNED(HttpStatus.FORBIDDEN, "/problems/member/banned", "영구 정지된 계정입니다.");

    private final HttpStatus status;
    private final String type;
    private final String message;

    MemberErrorCode(HttpStatus status, String type, String message) {
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
