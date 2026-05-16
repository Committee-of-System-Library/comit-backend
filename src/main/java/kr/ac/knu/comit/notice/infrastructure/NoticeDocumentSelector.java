package kr.ac.knu.comit.notice.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class NoticeDocumentSelector {

    static final int CONTEXT_NOTICE_COUNT = 5;
    static final double SIMILARITY_THRESHOLD = 0.4;

    public List<Document> select(List<Document> docs, List<Long> rerankedNoticeIds) {
        if (rerankedNoticeIds.isEmpty()) {
            return selectByThreshold(docs);
        }

        Map<Long, Document> documentByNoticeId = docs.stream()
                .filter(doc -> NoticeDocumentMetadata.parseNoticeId(doc) != null)
                .map(doc -> Map.entry(NoticeDocumentMetadata.parseNoticeId(doc), doc))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        List<Document> selectedDocs = rerankedNoticeIds.stream()
                .map(documentByNoticeId::get)
                .filter(Objects::nonNull)
                .limit(CONTEXT_NOTICE_COUNT)
                .toList();

        if (selectedDocs.isEmpty()) {
            return selectByThreshold(docs);
        }
        return selectedDocs;
    }

    private List<Document> selectByThreshold(List<Document> docs) {
        return docs.stream()
                .filter(this::isRelevantEnough)
                .limit(CONTEXT_NOTICE_COUNT)
                .toList();
    }

    private boolean isRelevantEnough(Document doc) {
        Double score = doc.getScore();
        return score == null || score >= SIMILARITY_THRESHOLD;
    }
}
