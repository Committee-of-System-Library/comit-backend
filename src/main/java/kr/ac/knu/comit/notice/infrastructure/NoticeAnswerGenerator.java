package kr.ac.knu.comit.notice.infrastructure;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class NoticeAnswerGenerator {

    private static final String SYSTEM_PROMPT = """
            당신은 경북대학교 컴퓨터학부 공지사항 도우미입니다.
            아래 공지사항 컨텍스트를 바탕으로 질문에 한국어로 간결하게 답변하세요.
            컨텍스트에 없는 내용은 모른다고 답하세요.
            사용자가 공지 목록을 물으면 제목, 대상, 기한처럼 구분에 필요한 핵심만 요약하세요.
            제출서류, 세부 일정, 금액 등 상세 정보는 사용자가 직접 물을 때만 답하세요.
            답변은 가능하면 5문장 이내로 작성하세요.
            
            [공지사항 컨텍스트]
            %s
            """;

    private final ChatClient chatClient;

    public NoticeAnswerGenerator(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public GeneratedAnswer generate(String message, List<Document> docs) {
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        ChatResponse response = chatClient.prompt()
                .system(SYSTEM_PROMPT.formatted(context))
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
