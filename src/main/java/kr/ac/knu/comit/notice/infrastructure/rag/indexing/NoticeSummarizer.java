package kr.ac.knu.comit.notice.infrastructure.rag.indexing;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class NoticeSummarizer {

    private final ChatClient chatClient;

    @Value("classpath:prompts/notice-summarize.st")
    private Resource summarizePrompt;

    public NoticeSummarizer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generate(String title, String content) {
        return chatClient.prompt()
                .system(summarizePrompt)
                .user(u -> u
                        .text("제목: {title}\n본문: {content}\n요약:")
                        .param("title", title)
                        .param("content", content)
                )
                .call()
                .content();
    }
}
