package kr.ac.knu.comit.notice.infrastructure.rag;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.document.Document;

final class NoticeDocumentMetadata {

    private NoticeDocumentMetadata() {
    }

    static Long parseNoticeId(Document doc) {
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

    static String title(Document doc) {
        return (String) doc.getMetadata().get("title");
    }

    static String originalUrl(Document doc) {
        return (String) doc.getMetadata().get("originalUrl");
    }

    static String deadlineDate(Document doc) {
        return (String) doc.getMetadata().get("deadlineDate");
    }

    static List<Document> distinctByNoticeId(List<Document> docs) {
        Map<Long, Document> documentsByNoticeId = new LinkedHashMap<>();
        for (Document doc : docs) {
            Long noticeId = parseNoticeId(doc);
            if (noticeId != null) {
                documentsByNoticeId.putIfAbsent(noticeId, doc);
            }
        }
        return List.copyOf(documentsByNoticeId.values());
    }

    static List<Long> noticeIds(List<Document> docs) {
        return docs.stream()
                .map(NoticeDocumentMetadata::parseNoticeId)
                .filter(Objects::nonNull)
                .toList();
    }
}
