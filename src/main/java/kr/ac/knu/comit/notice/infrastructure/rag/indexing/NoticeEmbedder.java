package kr.ac.knu.comit.notice.infrastructure.rag.indexing;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeDocumentText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeEmbedder {

    private final VectorStore vectorStore;
    private final NoticeChunker chunker;

    public void embed(Long noticeId, String wrId, String title, String content, String originalUrl) {
        String url = originalUrl != null ? originalUrl : "";
        List<String> chunks = chunker.chunk(content);

        LocalDate deadline = NoticeDeadlineExtractor.extract(content);

        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("noticeId", String.valueOf(noticeId));
            meta.put("wrId", wrId);
            meta.put("title", title);
            meta.put("originalUrl", url);
            meta.put("chunkIndex", String.valueOf(i));
            meta.put("chunkCount", String.valueOf(chunks.size()));
            if (deadline != null) {
                meta.put("deadlineDate", deadline.toString());
            }
            docs.add(Document.builder()
                    .id(toDocumentId(noticeId, i))
                    .text(NoticeDocumentText.format(title, chunks.get(i)))
                    .metadata(meta)
                    .build());
        }

        if (docs.isEmpty()) {
            log.debug("임베딩 스킵 - 청크 없음: noticeId={}", noticeId);
            return;
        }

        deleteLegacyDocument(noticeId);
        vectorStore.add(docs);
        log.debug("임베딩 저장 완료: noticeId={}, chunkCount={}", noticeId, docs.size());
    }

    private void deleteLegacyDocument(Long noticeId) {
        try {
            vectorStore.delete(List.of(toLegacyDocumentId(noticeId)));
        } catch (Exception e) {
            log.debug("기존 공지 단위 임베딩 삭제 스킵: noticeId={}", noticeId, e);
        }
    }

    private String toDocumentId(Long noticeId, int chunkIndex) {
        return UUID.nameUUIDFromBytes(("official-notice:%d:chunk:%d".formatted(noticeId, chunkIndex)).getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    private String toLegacyDocumentId(Long noticeId) {
        return UUID.nameUUIDFromBytes(("official-notice:" + noticeId).getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}
