package kr.ac.knu.comit.nightsnack.dto;

import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackSelectionStatus;

/** PENDING은 접수 완료이며 성공 보장이 아니다. 내 티켓 조회로 최종 결과를 확인한다. */
public record ApplyResponse(Long applicationId, NightSnackSelectionStatus selectionStatus, String ticketToken) {
    public static ApplyResponse from(NightSnackApplication application) {
        return new ApplyResponse(application.getId(), application.getSelectionStatus(), application.getAvailableTicketToken());
    }
}
