package kr.ac.knu.comit.notice.infrastructure.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoticeReranker {

    private static final int CONTEXT_NOTICE_COUNT = 5;
    private static final int RERANK_SUMMARY_PREVIEW_LENGTH = 180;

    private final ChatClient chatClient;
    private final OfficialNoticeRepository noticeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("classpath:prompts/notice-rerank.st")
    private Resource rerankPrompt;

    public NoticeReranker(ChatClient rerankClient,
            OfficialNoticeRepository noticeRepository) {
        this.chatClient = rerankClient;
        this.noticeRepository = noticeRepository;
    }

    public RerankedNotices rerank(String message, List<Document> docs) {
        if (docs.isEmpty()) {
            return new RerankedNotices(List.of(), null, null, null);
        }

        ChatResponse response = chatClient.prompt()
                .system(rerankPrompt)
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
        List<Document> candidateDocs = NoticeDocumentMetadata.distinctByNoticeId(docs);
        return """
                [사용자 질문]
                %s

                [검색 후보]
                %s
                """.formatted(message, buildCandidates(candidateDocs, findSummaries(candidateDocs)));
    }

    private String buildCandidates(List<Document> docs, Map<Long, String> summaries) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            Long noticeId = NoticeDocumentMetadata.parseNoticeId(doc);
            if (noticeId == null) {
                continue;
            }

            builder.append("noticeId=").append(noticeId).append('\n')
                    .append("score=").append(doc.getScore()).append('\n')
                    .append("title=").append(NoticeDocumentMetadata.title(doc)).append('\n')
                    .append("summary=").append(preview(summaries.get(noticeId))).append("\n\n");
        }

        return builder.toString();
    }

    private Map<Long, String> findSummaries(List<Document> docs) {
        List<Long> noticeIds = NoticeDocumentMetadata.noticeIds(docs);
        if (noticeIds.isEmpty()) {
            return Map.of();
        }

        return noticeRepository.findSummariesByIds(noticeIds).stream()
                .filter(summary -> summary.getSummary() != null && !summary.getSummary().isBlank())
                .collect(Collectors.toMap(
                        OfficialNoticeRepository.NoticeSummaryView::getId,
                        OfficialNoticeRepository.NoticeSummaryView::getSummary,
                        (left, right) -> left
                ));
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
        if (normalizedContent.length() <= RERANK_SUMMARY_PREVIEW_LENGTH) {
            return normalizedContent;
        }
        return normalizedContent.substring(0, RERANK_SUMMARY_PREVIEW_LENGTH);
    }

    private record RerankResponse(List<Long> noticeIds) {
    }
}
