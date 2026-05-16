package kr.ac.knu.comit.notice.infrastructure;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoticeChatClient {

    private static final int CONTEXT_NOTICE_COUNT = 5;
    private static final int TRACE_RETRIEVAL_COUNT = 20;
    private static final double SIMILARITY_THRESHOLD = 0.4;
    private static final String QUERY_TRANSFORM_PROMPT = """
            당신은 경북대학교 컴퓨터학부 공지사항 검색 쿼리 변환기입니다.
            사용자 질문의 핵심 명사를 최대한 유지하면서 검색에 불필요한 말만 제거하세요.
            답변하지 말고 검색 쿼리 한 줄만 출력하세요.
            오타, 띄어쓰기, 구어체, 축약어만 최소한으로 보정하세요.
            사용자가 쓰지 않은 학교명, 학과명, 기간, 금액, 서류, 대상, 지급일 같은 단어를 추가하지 마세요.
            기존 질문에 없는 연관 키워드를 추측해서 확장하지 마세요.
            키워드는 최대 4개까지만 사용하세요.
            """;
    private static final String SYSTEM_PROMPT = """
            당신은 경북대학교 컴퓨터학부 공지사항 도우미입니다.
            아래 공지사항 컨텍스트를 바탕으로 질문에 한국어로 간결하게 답변하세요.
            컨텍스트에 없는 내용은 모른다고 답하세요.
            
            [공지사항 컨텍스트]
            %s
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final SearchRequest traceSearchRequest;

    public NoticeChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.traceSearchRequest = SearchRequest.builder()
                .topK(TRACE_RETRIEVAL_COUNT)
                .build();
    }

    public ChatResult chat(String message) {
        String traceId = createTraceId();
        log.info("[RAG_TRACE] traceId={} step=query_received originalQuery=\"{}\" topK={} threshold={}",
                traceId, message, TRACE_RETRIEVAL_COUNT, SIMILARITY_THRESHOLD);

        TransformedQuery transformedQuery = transformQuery(message);
        String searchQuery = resolveSearchQuery(message, transformedQuery.content());
        log.info("[RAG_TRACE] traceId={} step=query_transformed transformedQuery=\"{}\" promptTokens={} completionTokens={} totalTokens={}",
                traceId,
                searchQuery,
                transformedQuery.promptTokens(),
                transformedQuery.completionTokens(),
                transformedQuery.totalTokens()
        );

        List<Document> retrievedDocs = searchRelatedNotices(searchQuery);
        traceRetrievedDocuments(traceId, retrievedDocs);

        List<Document> selectedDocs = selectContextDocuments(retrievedDocs);
        traceSelectedDocuments(traceId, selectedDocs);

        GeneratedAnswer generatedAnswer = generateAnswer(message, selectedDocs);
        String answer = generatedAnswer.content();
        List<NoticeSource> sources = toSources(selectedDocs);

        log.info("[RAG_TRACE] traceId={} step=answer_generated sourceNoticeIds={} answerLength={} promptTokens={} completionTokens={} totalTokens={}",
                traceId,
                sources.stream().map(NoticeSource::noticeId).toList(),
                answer != null ? answer.length() : 0,
                generatedAnswer.promptTokens(),
                generatedAnswer.completionTokens(),
                generatedAnswer.totalTokens()
        );
        return new ChatResult(answer, sources);
    }

    private TransformedQuery transformQuery(String message) {
        ChatResponse response = chatClient.prompt()
                .system(QUERY_TRANSFORM_PROMPT)
                .user(message)
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
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

    private List<Document> searchRelatedNotices(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.from(traceSearchRequest).query(query).build()
        );
    }

    private List<Document> selectContextDocuments(List<Document> docs) {
        return docs.stream()
                .filter(this::isRelevantEnough)
                .limit(CONTEXT_NOTICE_COUNT)
                .toList();
    }

    private boolean isRelevantEnough(Document doc) {
        Double score = doc.getScore();
        return score == null || score >= SIMILARITY_THRESHOLD;
    }

    private GeneratedAnswer generateAnswer(String message, List<Document> docs) {
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

    private void traceRetrievedDocuments(String traceId, List<Document> docs) {
        log.info("[RAG_TRACE] traceId={} step=retrieved count={}", traceId, docs.size());

        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            log.info("[RAG_TRACE] traceId={} step=retrieved_document rank={} score={} noticeId={} title=\"{}\" documentId={}",
                    traceId,
                    i + 1,
                    doc.getScore(),
                    doc.getMetadata().get("noticeId"),
                    doc.getMetadata().get("title"),
                    doc.getId()
            );
        }
    }

    private void traceSelectedDocuments(String traceId, List<Document> docs) {
        List<Long> noticeIds = docs.stream()
                .map(this::parseNoticeId)
                .filter(Objects::nonNull)
                .toList();

        log.info("[RAG_TRACE] traceId={} step=selected count={} noticeIds={}",
                traceId, docs.size(), noticeIds);
    }

    private List<NoticeSource> toSources(List<Document> docs) {
        return docs.stream()
                .map(this::toNoticeSource)
                .filter(Objects::nonNull)
                .toList();
    }

    private NoticeSource toNoticeSource(Document doc) {
        Long noticeId = parseNoticeId(doc);
        if (noticeId == null) {
            return null;
        }

        return new NoticeSource(
                noticeId,
                (String) doc.getMetadata().get("title"),
                (String) doc.getMetadata().get("originalUrl")
        );
    }

    private Long parseNoticeId(Document doc) {
        Object value = doc.getMetadata().get("noticeId");
        if (value == null) {
            return null;
        }

        String noticeId = value.toString();
        if (noticeId.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(noticeId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String createTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public record ChatResult(String answer, List<NoticeSource> sources) {
    }

    private record GeneratedAnswer(
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
    }

    private record TransformedQuery(
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
    }
}
