package kr.ac.knu.comit.notice.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.dto.CreateOfficialNoticeRequest;
import kr.ac.knu.comit.notice.dto.OfficialNoticeListResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeResponse;
import kr.ac.knu.comit.notice.dto.UpdateOfficialNoticeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficialNoticeService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OfficialNoticeRepository officialNoticeRepository;

    /**
     * cursor 기반으로 공지사항 목록을 최신순으로 조회한다.
     *
     * @apiNote cursor가 null이면 첫 페이지를 조회한다.
     */
    @Transactional(readOnly = true)
    public OfficialNoticeListResponse getNotices(Long cursorId, int size) {
        int pageSize = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, DEFAULT_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(0, pageSize + 1);

        List<OfficialNotice> notices = (cursorId == null)
                ? officialNoticeRepository.findFirstPage(pageable)
                : officialNoticeRepository.findByCursor(cursorId, pageable);

        return OfficialNoticeListResponse.of(notices, pageSize);
    }

    /**
     * 공지사항 하나의 상세 정보를 조회한다.
     */
    @Transactional(readOnly = true)
    public OfficialNoticeResponse getNotice(Long noticeId) {
        OfficialNotice notice = findActiveOrThrow(noticeId);
        return OfficialNoticeResponse.from(notice);
    }

    /**
     * 새 공지사항을 등록한다. (관리자 전용)
     */
    @Transactional
    public Long createNotice(CreateOfficialNoticeRequest request) {
        OfficialNotice notice = OfficialNotice.create(
                request.title(),
                request.content(),
                request.author(),
                request.originalUrl(),
                request.postedAt()
        );
        return officialNoticeRepository.save(notice).getId();
    }

    /**
     * 공지사항을 수정한다. (관리자 전용)
     */
    @Transactional
    public void updateNotice(Long noticeId, UpdateOfficialNoticeRequest request) {
        OfficialNotice notice = findActiveOrThrow(noticeId);
        notice.update(
                request.title(),
                request.content(),
                request.author(),
                request.originalUrl(),
                request.postedAt()
        );
    }

    /**
     * 공지사항을 소프트 삭제한다. (관리자 전용)
     */
    @Transactional
    public void deleteNotice(Long noticeId) {
        OfficialNotice notice = findActiveOrThrow(noticeId);
        notice.delete();
    }

    // ── internal helpers ─────────────────────────────────────

    private OfficialNotice findActiveOrThrow(Long noticeId) {
        return officialNoticeRepository.findActiveById(noticeId)
                .orElseThrow(() -> new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }
}
