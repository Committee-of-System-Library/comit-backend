package kr.ac.knu.comit.notice.infrastructure.rag;

public record GeneratedAnswer(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
