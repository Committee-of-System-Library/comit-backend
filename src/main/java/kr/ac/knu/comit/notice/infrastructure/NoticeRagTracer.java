package kr.ac.knu.comit.notice.infrastructure;

import java.util.List;
import java.util.UUID;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoticeRagTracer {

    public String createTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public void queryReceived(String traceId, String message, int topK, double threshold) {
        log.info("[RAG_TRACE] traceId={} step=query_received originalQuery=\"{}\" topK={} threshold={}",
                traceId, message, topK, threshold);
    }

    public void queryTransformed(String traceId, TransformedQuery transformedQuery) {
        log.info("[RAG_TRACE] traceId={} step=query_transformed transformedQuery=\"{}\" promptTokens={} completionTokens={} totalTokens={}",
                traceId,
                transformedQuery.content(),
                transformedQuery.promptTokens(),
                transformedQuery.completionTokens(),
                transformedQuery.totalTokens()
        );
    }

    public void retrievedDocuments(String traceId, List<Document> docs) {
        log.info("[RAG_TRACE] traceId={} step=retrieved count={}", traceId, docs.size());

        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            log.info("[RAG_TRACE] traceId={} step=retrieved_document rank={} score={} noticeId={} title=\"{}\" documentId={}",
                    traceId,
                    i + 1,
                    doc.getScore(),
                    doc.getMetadata().get("noticeId"),
                    NoticeDocumentMetadata.title(doc),
                    doc.getId()
            );
        }
    }

    public void reranked(String traceId, RerankedNotices rerankedNotices) {
        log.info("[RAG_TRACE] traceId={} step=reranked noticeIds={} promptTokens={} completionTokens={} totalTokens={}",
                traceId,
                rerankedNotices.noticeIds(),
                rerankedNotices.promptTokens(),
                rerankedNotices.completionTokens(),
                rerankedNotices.totalTokens()
        );
    }

    public void selectedDocuments(String traceId, List<Document> docs) {
        log.info("[RAG_TRACE] traceId={} step=selected count={} noticeIds={}",
                traceId, docs.size(), NoticeDocumentMetadata.noticeIds(docs));
    }

    public void queryClassified(String traceId, NoticeQueryType queryType, List<Document> selectedDocs, List<Document> answerDocs) {
        log.info("[RAG_TRACE] traceId={} step=query_classified queryType={} selectedCount={} answerSourceLimit={} answerNoticeIds={}",
                traceId,
                queryType,
                selectedDocs.size(),
                queryType.maxSources(),
                NoticeDocumentMetadata.noticeIds(answerDocs)
        );
    }

    public void answerGenerated(String traceId, GeneratedAnswer generatedAnswer, List<NoticeSource> sources) {
        String answer = generatedAnswer.content();
        log.info("[RAG_TRACE] traceId={} step=answer_generated sourceNoticeIds={} answerLength={} promptTokens={} completionTokens={} totalTokens={}",
                traceId,
                sources.stream().map(NoticeSource::noticeId).toList(),
                answer != null ? answer.length() : 0,
                generatedAnswer.promptTokens(),
                generatedAnswer.completionTokens(),
                generatedAnswer.totalTokens()
        );
    }
}
