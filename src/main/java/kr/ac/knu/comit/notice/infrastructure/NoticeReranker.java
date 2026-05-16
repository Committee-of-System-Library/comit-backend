package kr.ac.knu.comit.notice.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoticeReranker {

    private static final int CONTEXT_NOTICE_COUNT = 5;
    private static final int RERANK_CONTENT_PREVIEW_LENGTH = 300;
    private static final String RERANK_PROMPT = """
            당신은 경북대학교 컴퓨터학부 공지사항 검색 결과를 재정렬하는 평가자입니다.
            사용자 질문과 직접 관련 있는 공지만 고르세요.
            공지 제목, 본문 일부, 기존 벡터 검색 점수를 참고하세요.
            단어가 정확히 같지 않아도 같은 의미이면 관련 있다고 판단하세요.
            관련 없는 공지는 제외하세요.
            최대 5개까지 고르세요.
            응답은 반드시 JSON만 출력하세요.
            형식: {"noticeIds":[1,2,3]}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public NoticeReranker(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public RerankedNotices rerank(String message, List<Document> docs) {
        if (docs.isEmpty()) {
            return new RerankedNotices(List.of(), null, null, null);
        }

        ChatResponse response = chatClient.prompt()
                .system(RERANK_PROMPT)
                .user(buildUserPrompt(message, docs))
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();

        return new RerankedNotices(
                parseNoticeIds(content),
                usage != null ? usage.getPromptTokens() : null,
                usage != null ? usage.getCompletionTokens() : null,
                usage != null ? usage.getTotalTokens() : null
        );
    }

    private String buildUserPrompt(String message, List<Document> docs) {
        return """
                [사용자 질문]
                %s
                
                [검색 후보]
                %s
                """.formatted(message, buildCandidates(docs));
    }

    private String buildCandidates(List<Document> docs) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            Long noticeId = NoticeDocumentMetadata.parseNoticeId(doc);
            if (noticeId == null) {
                continue;
            }

            builder.append("rank=").append(i + 1).append('\n')
                    .append("noticeId=").append(noticeId).append('\n')
                    .append("score=").append(doc.getScore()).append('\n')
                    .append("title=").append(NoticeDocumentMetadata.title(doc)).append('\n')
                    .append("contentPreview=").append(preview(doc.getText())).append("\n\n");
        }

        return builder.toString();
    }

    private List<Long> parseNoticeIds(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        try {
            RerankResponse response = objectMapper.readValue(extractJson(content), RerankResponse.class);
            if (response.noticeIds() == null) {
                return List.of();
            }

            return response.noticeIds().stream()
                    .filter(Objects::nonNull)
                    .limit(CONTEXT_NOTICE_COUNT)
                    .toList();
        } catch (Exception e) {
            log.warn("[RAG_TRACE] step=rerank_parse_failed response=\"{}\"", content);
            return List.of();
        }
    }

    private String extractJson(String content) {
        int startIndex = content.indexOf('{');
        int endIndex = content.lastIndexOf('}');

        if (startIndex >= 0 && endIndex >= startIndex) {
            return content.substring(startIndex, endIndex + 1);
        }

        return content;
    }

    private String preview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalizedContent = content.replaceAll("\\s+", " ").strip();
        if (normalizedContent.length() <= RERANK_CONTENT_PREVIEW_LENGTH) {
            return normalizedContent;
        }
        return normalizedContent.substring(0, RERANK_CONTENT_PREVIEW_LENGTH);
    }

    private record RerankResponse(List<Long> noticeIds) {
    }
}
