package kr.ac.knu.comit.global.docs;

import java.util.List;

record GeneratedApiDocument(
        String title,
        String relativeOutputPath,
        String sectionPath,
        List<GeneratedEndpoint> endpoints
) {
}

record GeneratedEndpoint(
        String methodName,
        String summary,
        String description,
        String httpMethod,
        String path,
        List<FieldDoc> pathParameters,
        List<FieldDoc> queryParameters,
        List<FieldDoc> requestFields,
        List<FieldDoc> responseFields,
        List<ErrorDoc> errors,
        String requestExample,
        String responseExample,
        String errorExample
) {
}

record FieldDoc(
        String name,
        String type,
        boolean required,
        String description
) {
}

record ErrorDoc(
        String errorCode,
        int status,
        String type,
        String detail,
        String when
) {
}
