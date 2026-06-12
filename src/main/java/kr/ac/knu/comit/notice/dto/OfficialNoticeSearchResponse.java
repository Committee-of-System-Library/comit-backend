package kr.ac.knu.comit.notice.dto;

import kr.ac.knu.comit.notice.domain.OfficialNotice;

import java.util.List;

public record OfficialNoticeSearchResponse(
        List<OfficialNoticeSummaryResponse> notices
) {
    public static OfficialNoticeSearchResponse of(List<OfficialNotice> notices) {
        return new OfficialNoticeSearchResponse(
                notices.stream().map(OfficialNoticeSummaryResponse::from).toList()
        );
    }
}
