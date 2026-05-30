package kr.ac.knu.comit.notice.infrastructure.rag;

import java.util.List;
import java.util.Objects;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(NoticeRagProperties.class)
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

        TransformedQuery transformedQuery = queryTransformer.transform(message);
        ragTracer.queryTransformed(traceId, transformedQuery);

        List<Document> retrievedDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query(transformedQuery.content()).topK(topK).build()
        );
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
