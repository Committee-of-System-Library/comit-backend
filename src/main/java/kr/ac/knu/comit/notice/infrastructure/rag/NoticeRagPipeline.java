package kr.ac.knu.comit.notice.infrastructure.rag;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import kr.ac.knu.comit.notice.infrastructure.rag.config.NoticeRagProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
public class NoticeRagPipeline {

    private final VectorStore vectorStore;
    private final NoticeQueryTransformer queryTransformer;
    private final NoticeReranker reranker;
    private final NoticeDocumentSelector documentSelector;
    private final NoticeQueryTypeClassifier queryTypeClassifier;
    private final NoticeAnswerGenerator answerGenerator;
    private final NoticeRagTracer ragTracer;
    private final NoticeRagProperties properties;

    public NoticeRagPipeline(
            VectorStore vectorStore,
            NoticeQueryTransformer queryTransformer,
            NoticeReranker reranker,
            NoticeDocumentSelector documentSelector,
            NoticeQueryTypeClassifier queryTypeClassifier,
            NoticeAnswerGenerator answerGenerator,
            NoticeRagTracer ragTracer,
            NoticeRagProperties properties
    ) {
        this.vectorStore = vectorStore;
        this.queryTransformer = queryTransformer;
        this.reranker = reranker;
        this.documentSelector = documentSelector;
        this.queryTypeClassifier = queryTypeClassifier;
        this.answerGenerator = answerGenerator;
        this.ragTracer = ragTracer;
        this.properties = properties;
    }

    public ChatResult chat(String message) {
        int topK = properties.getRetrievalTopK();
        String traceId = ragTracer.createTraceId();
        ragTracer.queryReceived(traceId, message, topK, NoticeDocumentSelector.SIMILARITY_THRESHOLD);

        List<Document> retrievedDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query(message).topK(topK).build()
        );

        TransformedQuery transformedQuery;
        if (topScore(retrievedDocs) >= properties.getQueryTransformThreshold()) {
            transformedQuery = TransformedQuery.skipped(message);
        } else {
            transformedQuery = queryTransformer.transform(message);
            retrievedDocs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(transformedQuery.content()).topK(topK).build()
            );
        }
        ragTracer.queryTransformed(traceId, transformedQuery);
        ragTracer.retrievedDocuments(traceId, retrievedDocs);

        RerankedNotices rerankedNotices = reranker.rerank(message, retrievedDocs);
        ragTracer.reranked(traceId, rerankedNotices);

        List<Document> selectedDocs = documentSelector.select(retrievedDocs, rerankedNotices.noticeIds());
        ragTracer.selectedDocuments(traceId, selectedDocs);

        NoticeQueryType queryType = queryTypeClassifier.classify(message);
        List<Document> candidateDocs = queryType == NoticeQueryType.DEADLINE_SEARCH
                ? filterExpiredNotices(selectedDocs)
                : selectedDocs;
        List<Document> answerDocs = candidateDocs.stream()
                .limit(queryType.maxSources())
                .toList();
        ragTracer.queryClassified(traceId, queryType, selectedDocs, answerDocs);

        GeneratedAnswer generatedAnswer = answerGenerator.generate(message, answerDocs, queryType.answerTier());
        List<NoticeSource> sources = toSources(answerDocs);
        ragTracer.answerGenerated(traceId, generatedAnswer, sources);

        return new ChatResult(generatedAnswer.content(), sources);
    }

    private double topScore(List<Document> docs) {
        return docs.stream()
                .mapToDouble(doc -> doc.getScore() != null ? doc.getScore() : 0.0)
                .max()
                .orElse(0.0);
    }

    private List<NoticeSource> toSources(List<Document> docs) {
        return docs.stream()
                .map(this::toNoticeSource)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Document> filterExpiredNotices(List<Document> docs) {
        LocalDate today = LocalDate.now();
        return docs.stream()
                .filter(doc -> {
                    String deadlineDateStr = NoticeDocumentMetadata.deadlineDate(doc);
                    if (deadlineDateStr == null) {
                        return true;
                    }
                    try {
                        return !LocalDate.parse(deadlineDateStr).isBefore(today);
                    } catch (Exception e) {
                        return true;
                    }
                })
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
