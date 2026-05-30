package kr.ac.knu.comit.notice.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.infrastructure.KnuCseNoticeCrawler;
import kr.ac.knu.comit.notice.infrastructure.NoticeDetail;
import kr.ac.knu.comit.notice.infrastructure.NoticeEmbedder;
import kr.ac.knu.comit.notice.infrastructure.NoticeListItem;
import kr.ac.knu.comit.notice.infrastructure.NoticeSummarizer;
import kr.ac.knu.comit.notice.scheduler.config.NoticeSchedulerProperties;
import kr.ac.knu.comit.notice.service.OfficialNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(NoticeSchedulerProperties.class)
@RequiredArgsConstructor
public class OfficialNoticeScheduler {

    private final KnuCseNoticeCrawler crawler;
    private final OfficialNoticeRepository noticeRepository;
    private final OfficialNoticeService noticeService;
    private final NoticeEmbedder embedder;
    private final NoticeSummarizer summarizer;
    private final NoticeSchedulerProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (noticeRepository.count() == 0) {
            syncInitial();
        }

        if (properties.isReindexEmbeddingsOnStartup()) {
            reindexAll(properties.getReindexEmbeddingsLimit());
        }
    }

    private void syncInitial() {
        int page = 1;
        int saved = 0;

        while (saved < properties.getInitialSyncMax()) {
            List<NoticeListItem> items = crawler.crawlListPage(page++);
            if (items.isEmpty()) {
                break;
            }

            for (NoticeListItem item : items) {
                if (saved >= properties.getInitialSyncMax()) {
                    break;
                }

                processNotice(item);
                saved++;
            }
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void syncLatest() {
        for (int page = 1; page <= properties.getLatestSyncMaxPages(); page++) {
            List<NoticeListItem> items = crawler.crawlListPage(page);
            if (items.isEmpty()) {
                break;
            }

            List<String> wrIds = items.stream().map(NoticeListItem::wrId).toList();
            Set<String> existing = noticeRepository.findExistingWrIds(wrIds);

            SaveResult result = saveNewItems(items, existing);
            if (result.hitExisting()) {
                break;
            }
        }
    }

    private void processNotice(NoticeListItem item) {
        NoticeDetail detail = crawler.crawlDetail(item.wrId());
        LocalDateTime postedAt = resolvePostedAt(detail, item);

        String summary = summarizer.summarize(item.title(), detail.content());
        Long noticeId = noticeService.createNotice(
                item.wrId(), item.title(), detail.content(),
                item.author(), item.originalUrl(), postedAt, summary
        );

        embedder.embed(noticeId, item.wrId(), item.title(), detail.content(), item.originalUrl());
    }

    private void reindexAll(int limit) {
        List<OfficialNotice> notices = noticeRepository.findAllActive();
        if (limit > 0) {
            notices = notices.stream().limit(limit).toList();
        }

        for (OfficialNotice notice : notices) {
            embedder.embed(
                    notice.getId(),
                    notice.getWrId(),
                    notice.getTitle(),
                    notice.getContent(),
                    notice.getOriginalUrl()
            );
        }
    }

    private SaveResult saveNewItems(List<NoticeListItem> items, Set<String> existing) {
        int saved = 0;
        for (NoticeListItem item : items) {
            if (existing.contains(item.wrId())) {
                return new SaveResult(saved, true);
            }

            processNotice(item);
            saved++;
        }
        return new SaveResult(saved, false);
    }

    private LocalDateTime resolvePostedAt(NoticeDetail detail, NoticeListItem item) {
        if (detail.postedAt() != null) {
            return detail.postedAt();
        }
        if (item.postedDate() != null) {
            return item.postedDate().atStartOfDay();
        }
        return null;
    }

    private record SaveResult(int saved, boolean hitExisting) {
    }
}
