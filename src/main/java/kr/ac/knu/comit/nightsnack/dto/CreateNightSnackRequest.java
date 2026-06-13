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
        @NotNull
        LocalDateTime openAt,
        @NotNull
        LocalDateTime closeAt,
        @Size(max = 200)
        String title,
        String contents,
        @Size(max = 500)
        String menu,
        @Size(max = 200)
        String pickupLocation,
        LocalDateTime pickupStartTime,
        LocalDateTime pickupDeadline,
        boolean requiresStudentCouncilFee
) {
    public Period toPeriod() {
        return Period.of(openAt, closeAt);
    }
}
