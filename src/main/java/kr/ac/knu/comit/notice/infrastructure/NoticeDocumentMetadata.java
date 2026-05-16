package kr.ac.knu.comit.notice.infrastructure;

import java.util.List;
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

    static List<Long> noticeIds(List<Document> docs) {
        return docs.stream()
                .map(NoticeDocumentMetadata::parseNoticeId)
                .filter(Objects::nonNull)
                .toList();
    }
}
