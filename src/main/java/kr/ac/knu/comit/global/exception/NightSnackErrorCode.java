package kr.ac.knu.comit.global.exception;

import org.springframework.http.HttpStatus;

public enum NightSnackErrorCode implements ErrorCode {
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "/problems/night-snack/not-found", "존재하지 않는 야식 마차입니다."),
    EVENT_NOT_OPEN(HttpStatus.CONFLICT, "/problems/night-snack/not-open", "신청 가능한 상태가 아닙니다."),
    EVENT_SOLD_OUT(HttpStatus.CONFLICT, "/problems/night-snack/sold-out", "마감되었습니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "/problems/night-snack/already-applied", "이미 신청한 야식 마차입니다."),
    RESERVED_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "/problems/night-snack/reserved-capacity-exceeded",
            "예약 가능 인원을 초과했습니다."),
    RESERVATION_NOT_ALLOWED(HttpStatus.CONFLICT, "/problems/night-snack/reservation-not-allowed",
            "사전신청은 오픈 전(SCHEDULED) 상태에서만 가능합니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "/problems/night-snack/application-not-found",
            "신청 내역이 없습니다.");

    private final HttpStatus status;
    private final String type;
    private final String message;

    NightSnackErrorCode(HttpStatus status, String type, String message) {
        this.status = status;
        this.type = type;
        this.message = message;
    }

    @Override
    public int getStatus() {
        return status.value();
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return type;
    }
}
