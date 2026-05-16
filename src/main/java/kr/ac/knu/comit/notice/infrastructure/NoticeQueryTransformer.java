package kr.ac.knu.comit.notice.infrastructure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class NoticeQueryTransformer {

    private static final String QUERY_TRANSFORM_PROMPT = """
            당신은 경북대학교 컴퓨터학부 공지사항 검색 쿼리 변환기입니다.
            사용자 질문의 핵심 명사를 최대한 유지하면서 검색에 불필요한 말만 제거하세요.
            답변하지 말고 검색 쿼리 한 줄만 출력하세요.
            오타, 띄어쓰기, 구어체, 축약어만 최소한으로 보정하세요.
            사용자가 쓰지 않은 학교명, 학과명, 기간, 금액, 서류, 대상, 지급일 같은 단어를 추가하지 마세요.
            기존 질문에 없는 연관 키워드를 추측해서 확장하지 마세요.
            키워드는 최대 4개까지만 사용하세요.
            """;

    private final ChatClient chatClient;

    public NoticeQueryTransformer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public TransformedQuery transform(String message) {
        ChatResponse response = chatClient.prompt()
                .system(QUERY_TRANSFORM_PROMPT)
                .user(message)
                .call()
                .chatResponse();

        String content = resolveSearchQuery(message, response.getResult().getOutput().getText());
        Usage usage = response.getMetadata().getUsage();

        return new TransformedQuery(
                content,
                usage != null ? usage.getPromptTokens() : null,
                usage != null ? usage.getCompletionTokens() : null,
                usage != null ? usage.getTotalTokens() : null
        );
    }

    private String resolveSearchQuery(String originalQuery, String transformedQuery) {
        if (transformedQuery == null || transformedQuery.isBlank()) {
            return originalQuery;
        }
        return transformedQuery.strip();
    }
}
