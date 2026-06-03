package kr.ac.knu.comit.notice.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;
import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeRagPipeline;
import kr.ac.knu.comit.notice.infrastructure.rag.config.NoticeRagProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class NoticeChatService {

    private final NoticeRagPipeline noticeRagPipeline;
    private final ExecutorService ragVirtualThreadExecutor;
    private final Semaphore ragSemaphore;
    private final int acquireTimeoutSeconds;

    public NoticeChatService(
            NoticeRagPipeline noticeRagPipeline,
            @Qualifier("ragVirtualThreadExecutor") ExecutorService ragVirtualThreadExecutor,
            NoticeRagProperties properties
    ) {
        this.noticeRagPipeline = noticeRagPipeline;
        this.ragVirtualThreadExecutor = ragVirtualThreadExecutor;
        this.ragSemaphore = new Semaphore(properties.getChatMaxConcurrency());
        this.acquireTimeoutSeconds = properties.getChatAcquireTimeoutSeconds();
    }

    public CompletableFuture<NoticeChatResponse> chat(String message) {
        return CompletableFuture.supplyAsync(() -> {
            boolean acquired;
            try {
                acquired = ragSemaphore.tryAcquire(acquireTimeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(NoticeErrorCode.CHAT_UNAVAILABLE);
            }

            if (!acquired) {
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
