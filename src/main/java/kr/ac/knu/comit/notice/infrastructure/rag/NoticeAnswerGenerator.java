package kr.ac.knu.comit.notice.infrastructure.rag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final int MAX_CONTEXT_CHARS_PER_DOC = 600;

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
                .map(text -> text != null && text.length() > MAX_CONTEXT_CHARS_PER_DOC
                        ? text.substring(0, MAX_CONTEXT_CHARS_PER_DOC)
                        : text)
                .collect(Collectors.joining("\n\n---\n\n"));

        ChatClient chatClient = switch (tier) {
            case NANO -> nanoClient;
            case MINI -> miniClient;
            case FULL -> fullClient;
        };

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

        ChatResponse response = chatClient.prompt()
                .system(s -> s.text(answerPrompt)
                        .param("context", context)
                        .param("currentDate", currentDate))
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
