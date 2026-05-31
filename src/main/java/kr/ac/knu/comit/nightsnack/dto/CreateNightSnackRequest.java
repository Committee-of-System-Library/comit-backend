package kr.ac.knu.comit.nightsnack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record CreateNightSnackRequest(
        @NotNull
        LocalDate nightSnackDate,
        @Positive
        int capacity,
        /** 사전신청용 예약 정원(요구사항 4-3, 기본 10%). 생략 시 0(전량 일반 선착순). capacity 이하여야 한다. */
        @PositiveOrZero
        int reservedCapacity
) {
}
