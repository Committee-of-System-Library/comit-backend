package kr.ac.knu.comit.notice.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;
import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeRagPipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Service
public class NoticeChatService {

    private static final int MAX_CONCURRENT_RAG = 20;

    private final NoticeRagPipeline noticeRagPipeline;
    private final ExecutorService ragVirtualThreadExecutor;
    private final Semaphore ragSemaphore = new Semaphore(MAX_CONCURRENT_RAG);

    public NoticeChatService(
            NoticeRagPipeline noticeRagPipeline,
            @Qualifier("ragVirtualThreadExecutor") ExecutorService ragVirtualThreadExecutor
    ) {
        this.noticeRagPipeline = noticeRagPipeline;
        this.ragVirtualThreadExecutor = ragVirtualThreadExecutor;
    }

    public CompletableFuture<NoticeChatResponse> chat(String message) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ragSemaphore.tryAcquire()) {
                throw new BusinessException(NoticeErrorCode.CHAT_UNAVAILABLE);
            }
            try {
                NoticeRagPipeline.ChatResult result = noticeRagPipeline.chat(message);
                return NoticeChatResponse.of(result.answer(), result.sources());
            } finally {
                ragSemaphore.release();
            }
        }, ragVirtualThreadExecutor);
    }
}
