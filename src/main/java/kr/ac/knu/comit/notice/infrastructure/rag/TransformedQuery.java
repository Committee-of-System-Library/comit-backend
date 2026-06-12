package kr.ac.knu.comit.notice.infrastructure.rag;

public record TransformedQuery(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {

    public static TransformedQuery skipped(String originalQuery) {
        return new TransformedQuery(originalQuery, null, null, null);
    }
}
