package kr.ac.knu.comit.notice.infra;

import java.util.List;
import java.util.stream.Collectors;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
public class NoticeChatClient {

    private static final int CONTEXT_NOTICE_COUNT = 5;
    private static final String SYSTEM_PROMPT = """
            당신은 경북대학교 컴퓨터학부 공지사항 도우미입니다.
            아래 공지사항 컨텍스트를 바탕으로 질문에 한국어로 간결하게 답변하세요.
            컨텍스트에 없는 내용은 모른다고 답하세요.
            
            [공지사항 컨텍스트]
            %s
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;

    public NoticeChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.searchRequest = SearchRequest.builder()
                .topK(CONTEXT_NOTICE_COUNT)
                .build();
    }

    public ChatResult chat(String message) {
        List<Document> docs = searchRelatedNotices(message);
        String answer = generateAnswer(message, docs);
        List<NoticeSource> sources = toSources(docs);
        return new ChatResult(answer, sources);
    }

    private List<Document> searchRelatedNotices(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.from(searchRequest).query(query).build()
        );
    }

    private String generateAnswer(String message, List<Document> docs) {
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        return chatClient.prompt()
                .system(SYSTEM_PROMPT.formatted(context))
                .user(message)
                .call()
                .content();
    }

    private List<NoticeSource> toSources(List<Document> docs) {
        return docs.stream()
                .map(doc -> new NoticeSource(
                        ((Number) doc.getMetadata().get("noticeId")).longValue(),
                        (String) doc.getMetadata().get("title"),
                        (String) doc.getMetadata().get("originalUrl")
                ))
                .toList();
    }

    public record ChatResult(String answer, List<NoticeSource> sources) {
    }
}
