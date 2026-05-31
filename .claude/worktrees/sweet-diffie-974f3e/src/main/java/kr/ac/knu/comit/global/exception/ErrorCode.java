package kr.ac.knu.comit.global.exception;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();
    String getType();
}
