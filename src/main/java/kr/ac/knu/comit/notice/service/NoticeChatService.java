package kr.ac.knu.comit.notice.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeRagPipeline;
import kr.ac.knu.comit.notice.infrastructure.rag.config.NoticeRagProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NoticeChatService {

    private static final int DEFAULT_LATEST_NOTICE_COUNT = 5;
    private static final int MAX_LATEST_NOTICE_COUNT = 10;
    private static final int SUMMARY_PREVIEW_LENGTH = 120;
    private static final Pattern COUNT_PATTERN = Pattern.compile("(\\d{1,2})\\s*개");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final NoticeRagPipeline noticeRagPipeline;
    private final OfficialNoticeRepository officialNoticeRepository;
    private final ExecutorService ragVirtualThreadExecutor;
    private final Semaphore ragSemaphore;
    private final int acquireTimeoutSeconds;

    public NoticeChatService(
            NoticeRagPipeline noticeRagPipeline,
            OfficialNoticeRepository officialNoticeRepository,
            @Qualifier("ragVirtualThreadExecutor") ExecutorService ragVirtualThreadExecutor,
            NoticeRagProperties properties
    ) {
        this.noticeRagPipeline = noticeRagPipeline;
        this.officialNoticeRepository = officialNoticeRepository;
        this.ragVirtualThreadExecutor = ragVirtualThreadExecutor;
        this.ragSemaphore = new Semaphore(properties.getChatMaxConcurrency());
        this.acquireTimeoutSeconds = properties.getChatAcquireTimeoutSeconds();
    }

    public CompletableFuture<NoticeChatResponse> chat(String message) {
        return CompletableFuture.supplyAsync(() -> executeWithPermit(message), ragVirtualThreadExecutor);
    }

    private NoticeChatResponse executeWithPermit(String message) {
        acquirePermit();

        try {
            return generateResponse(message);
        } finally {
            ragSemaphore.release();
        }
    }

    private void acquirePermit() {
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
    }

    private NoticeChatResponse generateResponse(String message) {
        OptionalInt latestNoticeLimit = resolveLatestNoticeLimit(message);
        if (latestNoticeLimit.isPresent()) {
            return generateLatestNoticeResponse(latestNoticeLimit.getAsInt());
        }

        NoticeRagPipeline.ChatResult result = noticeRagPipeline.chat(message);
        return NoticeChatResponse.of(result.answer(), result.sources());
    }

    private OptionalInt resolveLatestNoticeLimit(String message) {
        if (message == null || message.isBlank()) {
            return OptionalInt.empty();
        }

        String normalized = message.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        String compact = normalized.replace(" ", "");
        boolean asksLatestNotice = compact.contains("공지")
                && (compact.contains("최신") || compact.contains("최근") || compact.contains("새로운") || compact.contains("새로올라온"));
        if (!asksLatestNotice) {
            return OptionalInt.empty();
        }

        Matcher matcher = COUNT_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return OptionalInt.of(DEFAULT_LATEST_NOTICE_COUNT);
        }

        int requestedCount = Integer.parseInt(matcher.group(1));
        int boundedCount = Math.max(1, Math.min(requestedCount, MAX_LATEST_NOTICE_COUNT));
        return OptionalInt.of(boundedCount);
    }

    private NoticeChatResponse generateLatestNoticeResponse(int limit) {
        List<OfficialNotice> notices = officialNoticeRepository.findFirstPage(PageRequest.of(0, limit));
        if (notices.isEmpty()) {
            return NoticeChatResponse.of("현재 확인된 공지사항이 없습니다.", List.of());
        }

        String answer = buildLatestNoticeAnswer(notices);
        List<NoticeSource> sources = notices.stream()
                .map(notice -> new NoticeSource(notice.getId(), notice.getTitle(), notice.getOriginalUrl()))
                .toList();
        return NoticeChatResponse.of(answer, sources);
    }

    private String buildLatestNoticeAnswer(List<OfficialNotice> notices) {
        StringBuilder builder = new StringBuilder("최신 공지 ")
                .append(notices.size())
                .append("개입니다.");

        for (OfficialNotice notice : notices) {
            builder.append('\n')
                    .append("• [")
                    .append(formatPostedAt(notice.getPostedAt()))
                    .append("] ")
                    .append(notice.getTitle());

            String summary = previewSummary(notice.getSummary());
            if (summary != null) {
                builder.append(" - ").append(summary);
            }
        }

        return builder.toString();
    }

    private String formatPostedAt(LocalDateTime postedAt) {
        if (postedAt == null) {
            return "게시일 미상";
        }
        return postedAt.toLocalDate().format(DATE_FORMATTER);
    }

    private String previewSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return null;
        }

        String normalizedSummary = summary.replaceAll("\\s+", " ").strip();
        if (normalizedSummary.length() <= SUMMARY_PREVIEW_LENGTH) {
            return normalizedSummary;
        }
        return normalizedSummary.substring(0, SUMMARY_PREVIEW_LENGTH) + "...";
    }
}
