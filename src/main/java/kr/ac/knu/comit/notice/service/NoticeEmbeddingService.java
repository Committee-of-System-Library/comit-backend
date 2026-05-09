package kr.ac.knu.comit.notice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeEmbeddingService {

    private final VectorStore vectorStore;

    public void embed(Long noticeId, String wrId, String title, String content, String originalUrl) {
        Document doc = Document.builder()
                .id(String.valueOf(noticeId))
                .text(title + "\n\n" + content)
                .metadata(Map.of(
                        "noticeId", noticeId,
                        "wrId", wrId,
                        "title", title,
                        "originalUrl", originalUrl != null ? originalUrl : ""
                ))
                .build();
        vectorStore.add(List.of(doc));
        log.debug("임베딩 저장 완료: noticeId={}", noticeId);
    }
}
