package kr.ac.knu.comit.notice.infrastructure.rag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            docs.add(Document.builder()
                    .id(toDocumentId(noticeId, i))
                    .text(NoticeDocumentText.format(title, chunks.get(i)))
                    .metadata(Map.of(
                            "noticeId", String.valueOf(noticeId),
                            "wrId", wrId,
                            "title", title,
                            "originalUrl", url,
                            "chunkIndex", String.valueOf(i),
                            "chunkCount", String.valueOf(chunks.size())
                    ))
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
