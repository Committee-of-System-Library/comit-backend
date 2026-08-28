package kr.ac.knu.comit.notice.scheduler;

import java.time.LocalDateTime;
import kr.ac.knu.comit.notice.infrastructure.crawler.KnuCseNoticeCrawler;
import kr.ac.knu.comit.notice.infrastructure.crawler.NoticeDetail;
import kr.ac.knu.comit.notice.infrastructure.crawler.NoticeListItem;
import kr.ac.knu.comit.notice.infrastructure.rag.indexing.NoticeEmbedder;
import kr.ac.knu.comit.notice.infrastructure.rag.indexing.NoticeSummarizer;
import kr.ac.knu.comit.notice.service.OfficialNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeProcessor {

    private final KnuCseNoticeCrawler crawler;
    private final OfficialNoticeService noticeService;
    private final NoticeSummarizer summarizer;
    private final NoticeEmbedder embedder;

    public void process(NoticeListItem item) {
        NoticeDetail detail = crawler.crawlDetail(item.wrId());

        if (detail.content() == null || detail.content().isBlank()) {
            log.debug("공지 본문 없음, 스킵: wrId={}, title={}", item.wrId(), item.title());
            return;
        }

        LocalDateTime postedAt = resolvePostedAt(detail, item);

        String summary = summarizer.generate(item.title(), detail.content());
        Long noticeId = noticeService.createNotice(
                item.wrId(), item.title(), detail.content(),
                item.author(), item.originalUrl(), postedAt, summary
        );

        embedder.embed(noticeId, item.wrId(), item.title(), detail.content(), item.originalUrl());
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
}
