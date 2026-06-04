package kr.ac.knu.comit.nightsnack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.domain.Period;

public record CreateNightSnackRequest(
        @NotNull
        LocalDate nightSnackDate,
        @Positive
        int capacity,
        /** 신청 오픈 시각. 관리자가 직접 지정하며, 스케줄러가 이 시각에 SCHEDULED → OPEN 전환한다. */
        @NotNull
        LocalDateTime openAt,
        /** 신청 마감 시각. openAt 이후여야 한다. */
        @NotNull
        LocalDateTime closeAt,
        @Size(max = 200)
        String title,
        String contents,
        @Size(max = 500)
        String menu,
        @Size(max = 200)
        String pickupLocation,
        /** 수령 마감 시각. 신청 마감(closeAt)과 별개로 현장 수령 가능 시각 상한을 나타낸다. */
        LocalDateTime pickupDeadline,
        /** 신청자격 — 학생회비 납부 여부. JSON에서 생략하면 false. */
        boolean requiresStudentCouncilFee
) {
    public Period toPeriod() {
        return Period.of(openAt, closeAt);
    }
}
