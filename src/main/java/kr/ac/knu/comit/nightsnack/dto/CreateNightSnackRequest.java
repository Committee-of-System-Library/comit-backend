package kr.ac.knu.comit.nightsnack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.domain.Period;

public record CreateNightSnackRequest(
        @NotNull
        LocalDate nightSnackDate,
        @Positive
        int capacity,
        /** 사전신청용 예약 정원(요구사항 4-3, 기본 10%). 생략 시 0(전량 일반 선착순). capacity 이하여야 한다. */
        @PositiveOrZero
        int reservedCapacity,
        /** 신청 오픈 시각. 관리자가 직접 지정하며, 스케줄러가 이 시각에 SCHEDULED → OPEN 전환한다. */
        @NotNull
        LocalDateTime openAt,
        /** 신청 마감 시각. openAt 이후여야 한다. */
        @NotNull
        LocalDateTime closeAt
) {
    public Period toPeriod() {
        return Period.of(openAt, closeAt);
    }
}
