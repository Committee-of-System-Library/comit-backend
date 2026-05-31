package kr.ac.knu.comit.global.exception;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;

final class ProblemDetailFactory {

    private ProblemDetailFactory() {
    }

    static ProblemDetail forBusiness(ErrorCode errorCode, String instance) {
        return base(errorCode, instance);
    }

    static ProblemDetail forValidation(MethodArgumentNotValidException exception, String instance) {
        ProblemDetail detail = base(CommonErrorCode.INVALID_REQUEST, instance);
        List<ProblemFieldViolation> invalidFields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ProblemFieldViolation(error.getField(), defaultMessage(error.getDefaultMessage())))
                .toList();
        detail.setProperty("invalidFields", invalidFields);
        return detail;
    }

    static ProblemDetail forUnexpected(String instance) {
        return forUnexpected(instance, UUID.randomUUID().toString());
    }

    static ProblemDetail forUnexpected(String instance, String trackingId) {
        ProblemDetail detail = base(CommonErrorCode.INTERNAL_SERVER_ERROR, instance);
        detail.setProperty("errorTrackingId", trackingId);
        return detail;
    }

    private static ProblemDetail base(ErrorCode errorCode, String instance) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(errorCode.getStatus()), errorCode.getMessage());
        detail.setTitle(HttpStatus.valueOf(errorCode.getStatus()).getReasonPhrase());
        detail.setType(URI.create(errorCode.getType()));
        detail.setInstance(URI.create(normalizeInstance(instance)));
        detail.setProperty("errorCode", errorCode.getCode());
        detail.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return detail;
    }

    private static String normalizeInstance(String instance) {
        return StringUtils.hasText(instance) ? instance : "/";
    }

    private static String defaultMessage(String message) {
        return StringUtils.hasText(message) ? message : "요청 값이 올바르지 않습니다.";
    }
}
