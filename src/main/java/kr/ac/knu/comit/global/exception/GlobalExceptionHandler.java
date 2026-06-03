package kr.ac.knu.comit.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        ProblemDetail problemDetail = ProblemDetailFactory.forBusiness(errorCode, request.getRequestURI());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.forValidation(e, request.getRequestURI());
        return ResponseEntity
                .status(problemDetail.getStatus())
                .body(problemDetail);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ProblemDetail> handleTimeoutException(TimeoutException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.forBusiness(NoticeErrorCode.CHAT_TIMEOUT, request.getRequestURI());
        return ResponseEntity
                .status(NoticeErrorCode.CHAT_TIMEOUT.getStatus())
                .body(problemDetail);
    }

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ProblemDetail> handleCompletionException(CompletionException e, HttpServletRequest request) {
        Throwable cause = e.getCause();
        if (cause instanceof BusinessException businessException) {
            return handleBusinessException(businessException, request);
        }
        if (cause instanceof TimeoutException timeoutException) {
            return handleTimeoutException(timeoutException, request);
        }
        String trackingId = UUID.randomUUID().toString();
        log.error("[CompletionException][{}] {}", trackingId, e.getMessage(), e);
        ProblemDetail problemDetail = ProblemDetailFactory.forUnexpected(request.getRequestURI(), trackingId);
        return ResponseEntity
                .status(problemDetail.getStatus())
                .body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception e, HttpServletRequest request) {
        String trackingId = UUID.randomUUID().toString();
        log.error("[UnexpectedException][{}] {}", trackingId, e.getMessage(), e);
        ProblemDetail problemDetail = ProblemDetailFactory.forUnexpected(request.getRequestURI(), trackingId);
        return ResponseEntity
                .status(problemDetail.getStatus())
                .body(problemDetail);
    }
}
