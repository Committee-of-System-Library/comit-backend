package kr.ac.knu.comit.notice.service;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.dto.OfficialNoticeListResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfficialNoticeService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OfficialNoticeRepository officialNoticeRepository;

    public OfficialNoticeListResponse getNotices(Long cursorId, int size) {
        int pageSize = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, DEFAULT_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(0, pageSize + 1);

        List<OfficialNotice> notices = (cursorId == null)
                ? officialNoticeRepository.findFirstPage(pageable)
                : officialNoticeRepository.findByCursor(cursorId, pageable);

        return OfficialNoticeListResponse.of(notices, pageSize);
    }

    public OfficialNoticeResponse getNotice(Long noticeId) {
        return OfficialNoticeResponse.from(findActiveOrThrow(noticeId));
    }

    @Transactional
    public Long createNotice(String wrId, String title, String content, String author,
                             String originalUrl, LocalDateTime postedAt) {
        OfficialNotice saveNotice = officialNoticeRepository.save(
                OfficialNotice.create(wrId, title, content, author, originalUrl, postedAt)
        );

        return saveNotice.getId();
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
