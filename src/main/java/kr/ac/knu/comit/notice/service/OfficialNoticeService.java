package kr.ac.knu.comit.notice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.dto.OfficialNoticeListResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfficialNoticeService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_SEARCH_SIZE = 20;

    private final OfficialNoticeRepository officialNoticeRepository;
    private final VectorStore vectorStore;

    public OfficialNoticeListResponse getNotices(Long cursorId, int size) {
        int pageSize = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, DEFAULT_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(0, pageSize + 1);

        List<OfficialNotice> notices = (cursorId == null)
                ? officialNoticeRepository.findFirstPage(pageable)
                : officialNoticeRepository.findByCursor(cursorId, pageable);

        return OfficialNoticeListResponse.of(notices, pageSize);
    }

    public OfficialNoticeSearchResponse searchNotices(String query, int size) {
        int topK = Math.min(size <= 0 ? MAX_SEARCH_SIZE : size, MAX_SEARCH_SIZE);

        List<Long> noticeIds = vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).topK(topK).build()
                ).stream()
                .map(doc -> ((Number) doc.getMetadata().get("noticeId")).longValue())
                .toList();

        Map<Long, OfficialNotice> noticeMap = officialNoticeRepository.findAllById(noticeIds).stream()
                .collect(Collectors.toMap(OfficialNotice::getId, Function.identity()));

        List<OfficialNotice> ordered = noticeIds.stream()
                .filter(noticeMap::containsKey)
                .map(noticeMap::get)
                .toList();

        return OfficialNoticeSearchResponse.of(ordered);
    }

    public OfficialNoticeResponse getNotice(Long noticeId) {
        return OfficialNoticeResponse.from(findActiveOrThrow(noticeId));
    }

    @Transactional
    public Long createNotice(String wrId, String title, String content, String author,
                             String originalUrl, LocalDateTime postedAt, String summary) {
        OfficialNotice notice = officialNoticeRepository.save(
                OfficialNotice.create(wrId, title, content, author, originalUrl, postedAt, summary)
        );
        return notice.getId();
    }

    @Transactional
    public void updateNotice(Long noticeId, String title, String content, String author,
                             String originalUrl, LocalDateTime postedAt) {
        findActiveOrThrow(noticeId).update(title, content, author, originalUrl, postedAt);
    }

    private OfficialNotice findActiveOrThrow(Long noticeId) {
        return officialNoticeRepository.findActiveById(noticeId)
                .orElseThrow(() -> new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }
}
