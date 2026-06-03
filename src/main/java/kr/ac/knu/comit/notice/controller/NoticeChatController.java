package kr.ac.knu.comit.notice.controller;

import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.notice.controller.api.NoticeChatControllerApi;
import kr.ac.knu.comit.notice.dto.NoticeChatRequest;
import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infrastructure.rag.config.NoticeRagProperties;
import kr.ac.knu.comit.notice.service.NoticeChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
public class NoticeChatController implements NoticeChatControllerApi {

    private final NoticeChatService noticeChatService;
    private final int responseTimeoutSeconds;

    public NoticeChatController(NoticeChatService noticeChatService, NoticeRagProperties properties) {
        this.noticeChatService = noticeChatService;
        this.responseTimeoutSeconds = properties.getChatResponseTimeoutSeconds();
    }

    @Override
    public CompletableFuture<ResponseEntity<ApiResponse<NoticeChatResponse>>> chat(
            @AuthenticatedMember MemberPrincipal principal,
            NoticeChatRequest request) {
        return noticeChatService.chat(request.message())
                .orTimeout(responseTimeoutSeconds, TimeUnit.SECONDS)
                .thenApply(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }
}
