package kr.ac.knu.comit.nightsnack.dto;

import java.util.List;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;

/**
 * 사전신청 배치 등록 결과(요구사항 4-3).
 *
 * @param requested 요청한 학번 수(중복 제거 후)
 * @param registered 이번에 신규로 등록된 수
 * @param skipped 이미 등록되어 건너뛴 수
 * @param tickets 신규 등록된 사전신청자에게 발급된 티켓 목록(현장 배부용)
 */
public record ReserveResponse(
        int requested,
        int registered,
        int skipped,
        List<ReservedTicket> tickets
) {

    public static ReserveResponse of(int requested, List<NightSnackApplication> created) {
        List<ReservedTicket> tickets = created.stream().map(ReservedTicket::from).toList();
        return new ReserveResponse(requested, tickets.size(), requested - tickets.size(), tickets);
    }

    public record ReservedTicket(String studentNumber, String ticketToken) {

        public static ReservedTicket from(NightSnackApplication application) {
            return new ReservedTicket(application.getStudentNumber(), application.getTicketToken());
        }
    }
}
