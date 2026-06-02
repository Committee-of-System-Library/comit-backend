package kr.ac.knu.comit.notice.service;

import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeRagPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class NoticeChatService {

    private final NoticeRagPipeline noticeRagPipeline;
    private final ExecutorService ragVirtualThreadExecutor;

    public CompletableFuture<NoticeChatResponse> chat(String message) {
        return CompletableFuture.supplyAsync(() -> {
            NoticeRagPipeline.ChatResult result = noticeRagPipeline.chat(message);
            return NoticeChatResponse.of(result.answer(), result.sources());
        }, ragVirtualThreadExecutor);
    }
}
