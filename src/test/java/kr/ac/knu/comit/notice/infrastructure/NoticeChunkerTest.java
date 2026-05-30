package kr.ac.knu.comit.notice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeChunker;
import org.junit.jupiter.api.Test;

class NoticeChunkerTest {

    private final NoticeChunker chunker = new NoticeChunker();

    @Test
    void keepShortContentAsSingleChunk() {
        List<String> chunks = chunker.chunk("공지 내용 ".repeat(100));

        assertThat(chunks).hasSize(1);
    }

    @Test
    void keepSmallTailAsSingleChunk() {
        List<String> chunks = chunker.chunk("가".repeat(1_050));

        assertThat(chunks).hasSize(1);
    }

    @Test
    void splitLongContentWithOverlap() {
        String content = "가".repeat(1_200);

        List<String> chunks = chunker.chunk(content);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(900);
        assertThat(chunks.get(1)).hasSize(450);
    }
}
