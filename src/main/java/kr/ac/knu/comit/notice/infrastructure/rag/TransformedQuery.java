package kr.ac.knu.comit.notice.infrastructure.rag;

public record TransformedQuery(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
