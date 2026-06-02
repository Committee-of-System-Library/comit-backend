package kr.ac.knu.comit.notice.scheduler;

import java.time.LocalDateTime;
import kr.ac.knu.comit.notice.infrastructure.crawler.KnuCseNoticeCrawler;
import kr.ac.knu.comit.notice.infrastructure.crawler.NoticeDetail;
import kr.ac.knu.comit.notice.infrastructure.crawler.NoticeListItem;
import kr.ac.knu.comit.notice.infrastructure.rag.indexing.NoticeEmbedder;
import kr.ac.knu.comit.notice.infrastructure.rag.indexing.NoticeSummarizer;
import kr.ac.knu.comit.notice.service.OfficialNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeProcessor {

    private final KnuCseNoticeCrawler crawler;
    private final OfficialNoticeService noticeService;
    private final NoticeSummarizer summarizer;
    private final NoticeEmbedder embedder;

    public void process(NoticeListItem item) {
        NoticeDetail detail = crawler.crawlDetail(item.wrId());
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
