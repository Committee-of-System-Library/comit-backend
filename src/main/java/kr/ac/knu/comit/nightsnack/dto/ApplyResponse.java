package kr.ac.knu.comit.nightsnack.dto;

import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;

/**
 * 신청 성공 응답.
 *
 * @param ticketToken QR 수령 티켓 값
 * @param sequence    몇 번째 신청자인지 (요구사항: "자기가 몇 번째인지 보여주면 된다")
 * @param remaining   응답 시점의 잔여 수량 스냅샷
 */
public record ApplyResponse(String ticketToken, int sequence, int remaining) {

    public static ApplyResponse of(NightSnackApplication application, int sequence, int remaining) {
        return new ApplyResponse(application.getTicketToken(), sequence, remaining);
    }
}
