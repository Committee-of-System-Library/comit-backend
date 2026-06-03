package kr.ac.knu.comit.notice.service;

import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeRagPipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class NoticeChatService {

    private final NoticeRagPipeline noticeRagPipeline;
    private final ExecutorService ragVirtualThreadExecutor;

    public NoticeChatService(
            NoticeRagPipeline noticeRagPipeline,
            @Qualifier("ragVirtualThreadExecutor") ExecutorService ragVirtualThreadExecutor
    ) {
        this.noticeRagPipeline = noticeRagPipeline;
        this.ragVirtualThreadExecutor = ragVirtualThreadExecutor;
    }

    public CompletableFuture<NoticeChatResponse> chat(String message) {
        return CompletableFuture.supplyAsync(() -> {
            NoticeRagPipeline.ChatResult result = noticeRagPipeline.chat(message);
            return NoticeChatResponse.of(result.answer(), result.sources());
        }, ragVirtualThreadExecutor);
    }
}
