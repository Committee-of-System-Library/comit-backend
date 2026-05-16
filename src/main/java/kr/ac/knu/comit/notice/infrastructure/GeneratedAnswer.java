package kr.ac.knu.comit.notice.infrastructure;

public record GeneratedAnswer(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
