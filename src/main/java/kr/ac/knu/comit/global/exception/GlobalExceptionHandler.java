package kr.ac.knu.comit.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.forBusiness(CommonErrorCode.INVALID_REQUEST, request.getRequestURI());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.forBusiness(CommonErrorCode.INVALID_REQUEST, request.getRequestURI());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.forBusiness(CommonErrorCode.INVALID_REQUEST, request.getRequestURI());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.forBusiness(CommonErrorCode.INVALID_REQUEST, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
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
