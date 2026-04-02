package kr.ac.knu.comit.global.exception;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ErrorCodeRegistry {

    private static final Map<String, ErrorCode> ERROR_CODES = registerAll();

    private ErrorCodeRegistry() {
    }

    public static ErrorCode require(String code) {
        ErrorCode errorCode = ERROR_CODES.get(code);
        if (errorCode == null) {
            throw new IllegalArgumentException("Unknown error code: " + code);
        }
        return errorCode;
    }

    private static Map<String, ErrorCode> registerAll() {
        LinkedHashMap<String, ErrorCode> registry = new LinkedHashMap<>();
        register(registry, CommonErrorCode.values());
        register(registry, MemberErrorCode.values());
        register(registry, PostErrorCode.values());
        register(registry, CommentErrorCode.values());
        register(registry, ReportErrorCode.values());
        register(registry, StorageErrorCode.values());
        return Map.copyOf(registry);
    }

    private static void register(Map<String, ErrorCode> registry, ErrorCode[] codes) {
        Arrays.stream(codes).forEach(code -> {
            ErrorCode previous = registry.putIfAbsent(code.getCode(), code);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate error code: " + code.getCode()
                                + " (" + previous.getClass().getSimpleName()
                                + " vs " + code.getClass().getSimpleName() + ")"
                );
            }
        });
    }
}
