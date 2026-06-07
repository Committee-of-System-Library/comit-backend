package kr.ac.knu.comit.notice.scheduler;

import java.util.List;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.infrastructure.crawler.NoticeListItem;
import kr.ac.knu.comit.notice.infrastructure.rag.indexing.NoticeEmbedder;
import kr.ac.knu.comit.notice.scheduler.config.NoticeSchedulerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import kr.ac.knu.comit.notice.infrastructure.crawler.KnuCseNoticeCrawler;

@Component
@RequiredArgsConstructor
public class OfficialNoticeInitializer {

    private static final int INITIAL_SYNC_MAX_PAGES = 150;

    private final KnuCseNoticeCrawler crawler;
    private final OfficialNoticeRepository noticeRepository;
    private final NoticeEmbedder embedder;
    private final NoticeProcessor noticeProcessor;
    private final NoticeSchedulerProperties properties;

    @Async
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
        for (int page = 1; page <= INITIAL_SYNC_MAX_PAGES; page++) {
            List<NoticeListItem> items = crawler.crawlListPage(page);
            if (items.isEmpty()) {
                break;
            }

            for (NoticeListItem item : items) {
                noticeProcessor.process(item);
            }
        }
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
}
