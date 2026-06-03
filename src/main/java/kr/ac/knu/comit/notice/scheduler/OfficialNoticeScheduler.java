package kr.ac.knu.comit.notice.scheduler;

import java.util.List;
import java.util.Set;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.infrastructure.crawler.KnuCseNoticeCrawler;
import kr.ac.knu.comit.notice.infrastructure.crawler.NoticeListItem;
import kr.ac.knu.comit.notice.scheduler.config.NoticeSchedulerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficialNoticeScheduler {

    private final KnuCseNoticeCrawler crawler;
    private final OfficialNoticeRepository noticeRepository;
    private final NoticeProcessor noticeProcessor;
    private final NoticeSchedulerProperties properties;

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

    private SaveResult saveNewItems(List<NoticeListItem> items, Set<String> existing) {
        int saved = 0;
        for (NoticeListItem item : items) {
            if (existing.contains(item.wrId())) {
                return new SaveResult(saved, true);
            }

            noticeProcessor.process(item);
            saved++;
        }
        return new SaveResult(saved, false);
    }

    private record SaveResult(int saved, boolean hitExisting) {
    }
}
