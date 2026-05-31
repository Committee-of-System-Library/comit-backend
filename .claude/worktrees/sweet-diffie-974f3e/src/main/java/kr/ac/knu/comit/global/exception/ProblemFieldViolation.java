package kr.ac.knu.comit.global.exception;

public record ProblemFieldViolation(
        String field,
        String message
) {
}
