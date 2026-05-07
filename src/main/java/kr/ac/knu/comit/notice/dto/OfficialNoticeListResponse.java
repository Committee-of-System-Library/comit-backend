package kr.ac.knu.comit.notice.dto;

import kr.ac.knu.comit.notice.domain.OfficialNotice;

import java.util.List;

public record OfficialNoticeListResponse(
        List<OfficialNoticeSummaryResponse> notices,
        Long nextCursorId,
        boolean hasNext
) {
    public static OfficialNoticeListResponse of(List<OfficialNotice> notices, int requestedSize) {
        boolean hasNext = notices.size() > requestedSize;
        List<OfficialNotice> page = hasNext ? notices.subList(0, requestedSize) : notices;
        Long nextCursorId = hasNext ? page.get(page.size() - 1).getId() : null;

        return new OfficialNoticeListResponse(
                page.stream().map(OfficialNoticeSummaryResponse::from).toList(),
                nextCursorId,
                hasNext
        );
    }
}
