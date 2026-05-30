package kr.ac.knu.comit.notice.infrastructure.rag;

import java.util.List;
import java.util.Objects;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
public class NoticeChatClient {

    private static final int TRACE_RETRIEVAL_COUNT = 10;

    private final VectorStore vectorStore;
    private final NoticeQueryTransformer queryTransformer;
    private final NoticeReranker reranker;
    private final NoticeDocumentSelector documentSelector;
    private final NoticeQueryTypeClassifier queryTypeClassifier;
    private final NoticeAnswerGenerator answerGenerator;
    private final NoticeRagTracer ragTracer;
    private final SearchRequest traceSearchRequest;

    public NoticeChatClient(
            VectorStore vectorStore,
            NoticeQueryTransformer queryTransformer,
            NoticeReranker reranker,
            NoticeDocumentSelector documentSelector,
            NoticeQueryTypeClassifier queryTypeClassifier,
            NoticeAnswerGenerator answerGenerator,
            NoticeRagTracer ragTracer
    ) {
        this.vectorStore = vectorStore;
        this.queryTransformer = queryTransformer;
        this.reranker = reranker;
        this.documentSelector = documentSelector;
        this.queryTypeClassifier = queryTypeClassifier;
        this.answerGenerator = answerGenerator;
        this.ragTracer = ragTracer;
        this.traceSearchRequest = SearchRequest.builder()
                .topK(TRACE_RETRIEVAL_COUNT)
                .build();
    }

    public ChatResult chat(String message) {
        String traceId = ragTracer.createTraceId();
        ragTracer.queryReceived(
                traceId,
                message,
                TRACE_RETRIEVAL_COUNT,
                NoticeDocumentSelector.SIMILARITY_THRESHOLD
        );

        TransformedQuery transformedQuery = queryTransformer.transform(message);
        ragTracer.queryTransformed(traceId, transformedQuery);

        List<Document> retrievedDocs = searchRelatedNotices(transformedQuery.content());
        ragTracer.retrievedDocuments(traceId, retrievedDocs);

        RerankedNotices rerankedNotices = reranker.rerank(message, retrievedDocs);
        ragTracer.reranked(traceId, rerankedNotices);

        List<Document> selectedDocs = documentSelector.select(retrievedDocs, rerankedNotices.noticeIds());
        ragTracer.selectedDocuments(traceId, selectedDocs);

        NoticeQueryType queryType = queryTypeClassifier.classify(message);
        List<Document> answerDocs = selectedDocs.stream()
                .limit(queryType.maxSources())
                .toList();
        ragTracer.queryClassified(traceId, queryType, selectedDocs, answerDocs);

        GeneratedAnswer generatedAnswer = answerGenerator.generate(message, answerDocs);
        List<NoticeSource> sources = toSources(answerDocs);
        ragTracer.answerGenerated(traceId, generatedAnswer, sources);

        return new ChatResult(generatedAnswer.content(), sources);
    }

    private List<Document> searchRelatedNotices(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.from(traceSearchRequest).query(query).build()
        );
    }

    private List<NoticeSource> toSources(List<Document> docs) {
        return docs.stream()
                .map(this::toNoticeSource)
                .filter(Objects::nonNull)
                .toList();
    }

    private NoticeSource toNoticeSource(Document doc) {
        Long noticeId = NoticeDocumentMetadata.parseNoticeId(doc);
        if (noticeId == null) {
            return null;
        }

        return new NoticeSource(
                noticeId,
                NoticeDocumentMetadata.title(doc),
                NoticeDocumentMetadata.originalUrl(doc)
        );
    }

    public record ChatResult(String answer, List<NoticeSource> sources) {
    }
}
