package kr.ac.knu.comit.notice.infrastructure.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class NoticeQueryTransformer {

    private final ChatClient chatClient;

    @Value("classpath:prompts/notice-query-transform.st")
    private Resource queryTransformPrompt;

    public NoticeQueryTransformer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public TransformedQuery transform(String message) {
        ChatResponse response = chatClient.prompt()
                .system(queryTransformPrompt)
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
