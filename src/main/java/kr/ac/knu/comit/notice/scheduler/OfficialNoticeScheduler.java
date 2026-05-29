package kr.ac.knu.comit.notice.scheduler;

import java.io.IOException;
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
    public void syncInitial() {
        if (noticeRepository.count() > 0) {
            if (properties.isReindexEmbeddingsOnStartup()) {
                reindexEmbeddings();
            }
            log.info("초기 크롤링 스킵 - 이미 데이터 존재");
            return;
        }

        log.info("초기 크롤링 시작 - 최대 {}개", properties.getInitialSyncMax());
        int page = 1;
        int saved = 0;

        while (saved < properties.getInitialSyncMax()) {
            List<NoticeListItem> items = crawlListPageSafely(page++);
            if (items == null || items.isEmpty()) {
                break;
            }

            for (NoticeListItem item : items) {
                if (saved >= properties.getInitialSyncMax()) {
                    break;
                }
                if (saveNotice(item)) {
                    saved++;
                }
            }
        }

        log.info("초기 크롤링 완료 - {}개 저장", saved);
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void syncLatest() {
        log.info("최신 공지 동기화 시작");
        int newCount = 0;

        for (int page = 1; page <= properties.getLatestSyncMaxPages(); page++) {
            List<NoticeListItem> items = crawlListPageSafely(page);
            if (items == null || items.isEmpty()) {
                break;
            }

            List<String> wrIds = items.stream().map(NoticeListItem::wrId).toList();
            Set<String> existing = noticeRepository.findExistingWrIds(wrIds);

            SaveResult result = saveNewItems(items, existing);
            newCount += result.saved();
            if (result.hitExisting()) {
                break;
            }
        }

        log.info("최신 공지 동기화 완료 - {}개 저장", newCount);
    }

    private boolean saveNotice(NoticeListItem item) {
        try {
            NoticeDetail detail = crawler.crawlDetail(item.wrId());
            LocalDateTime postedAt = resolvePostedAt(detail, item);
            String summary = summarizeSafely(item.title(), detail.content());
            Long noticeId = noticeService.createNotice(
                    item.wrId(), item.title(), detail.content(),
                    item.author(), item.originalUrl(), postedAt, summary
            );
            embedSafely(noticeId, item, detail.content());
            return true;
        } catch (Exception e) {
            log.warn("공지 저장 실패: wrId={}", item.wrId(), e);
            return false;
        }
    }

    private List<NoticeListItem> crawlListPageSafely(int page) {
        try {
            return crawler.crawlListPage(page);
        } catch (IOException e) {
            log.error("목록 페이지 크롤링 실패: page={}", page, e);
            return null;
        }
    }

    private SaveResult saveNewItems(List<NoticeListItem> items, Set<String> existing) {
        int saved = 0;
        for (NoticeListItem item : items) {
            if (existing.contains(item.wrId())) {
                return new SaveResult(saved, true);
            }
            if (saveNotice(item)) {
                saved++;
            }
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

    private String summarizeSafely(String title, String content) {
        try {
            return summarizer.summarize(title, content);
        } catch (Exception e) {
            log.warn("요약 실패: title={}", title, e);
            return null;
        }
    }

    private void embedSafely(Long noticeId, NoticeListItem item, String content) {
        try {
            embedder.embed(noticeId, item.wrId(), item.title(), content, item.originalUrl());
        } catch (Exception e) {
            log.warn("임베딩 실패: noticeId={}, wrId={}", noticeId, item.wrId(), e);
        }
    }

    private void reindexEmbeddings() {
        List<OfficialNotice> notices = noticeRepository.findAllActive();
        if (properties.getReindexEmbeddingsLimit() > 0) {
            notices = notices.stream()
                    .limit(properties.getReindexEmbeddingsLimit())
                    .toList();
        }
        log.info("공지 임베딩 재색인 시작 - count={}", notices.size());
        int embedded = 0;
        for (OfficialNotice notice : notices) {
            try {
                embedder.embed(
                        notice.getId(),
                        notice.getWrId(),
                        notice.getTitle(),
                        notice.getContent(),
                        notice.getOriginalUrl()
                );
                embedded++;
            } catch (Exception e) {
                log.warn("공지 임베딩 재색인 실패: noticeId={}, wrId={}", notice.getId(), notice.getWrId(), e);
            }
        }
        log.info("공지 임베딩 재색인 완료 - embedded={}", embedded);
    }

    private record SaveResult(int saved, boolean hitExisting) {
    }
}
