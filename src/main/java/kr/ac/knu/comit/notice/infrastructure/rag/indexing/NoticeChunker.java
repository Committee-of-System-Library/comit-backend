package kr.ac.knu.comit.notice.infrastructure.rag.indexing;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoticeChunker {

    private static final int CHUNK_SIZE = 900;
    private static final int CHUNK_OVERLAP = 150;
    private static final int MIN_TAIL_SIZE = 200;

    public List<String> chunk(String content) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return List.of();
        }

        if (isSmall(normalized)) {
            return List.of(normalized);
        }

        return split(normalized);
    }

    private boolean isSmall(String content) {
        return content.length() <= CHUNK_SIZE + MIN_TAIL_SIZE;
    }

    private List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        int startIndex = 0;

        while (startIndex < content.length()) {
            int endIndex = Math.min(startIndex + CHUNK_SIZE, content.length());
            if (content.length() - endIndex < MIN_TAIL_SIZE) {
                endIndex = content.length();
            }

            chunks.add(content.substring(startIndex, endIndex).strip());
            if (endIndex == content.length()) {
                break;
            }

            startIndex = endIndex - CHUNK_OVERLAP;
        }

        return chunks;
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("\\s+", " ").strip();
    }
}
