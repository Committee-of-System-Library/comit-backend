package kr.ac.knu.comit.docs;

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
        String httpMethod,
        String path,
        List<FieldDoc> pathParameters,
        List<FieldDoc> queryParameters,
        List<FieldDoc> requestFields,
        List<FieldDoc> responseFields,
        String requestExample,
        String responseExample
) {
}

record FieldDoc(
        String name,
        String type,
        boolean required,
        String description
) {
}
