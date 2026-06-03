package kr.ac.knu.comit.notice.infrastructure.rag;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class NoticeAnswerGenerator {

    private final ChatClient nanoClient;
    private final ChatClient miniClient;
    private final ChatClient fullClient;

    @Value("classpath:prompts/notice-answer.st")
    private Resource answerPrompt;

    public NoticeAnswerGenerator(
            ChatClient answerNanoClient,
            ChatClient answerMiniClient,
            ChatClient answerClient
    ) {
        this.nanoClient = answerNanoClient;
        this.miniClient = answerMiniClient;
        this.fullClient = answerClient;
    }

    public GeneratedAnswer generate(String message, List<Document> docs, AnswerTier tier) {
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        ChatClient chatClient = switch (tier) {
            case NANO -> nanoClient;
            case MINI -> miniClient;
            case FULL -> fullClient;
        };

        ChatResponse response = chatClient.prompt()
                .system(s -> s.text(answerPrompt).param("context", context))
                .user(message)
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();

        return new GeneratedAnswer(
                content,
                usage != null ? usage.getPromptTokens() : null,
                usage != null ? usage.getCompletionTokens() : null,
                usage != null ? usage.getTotalTokens() : null
        );
    }
}
