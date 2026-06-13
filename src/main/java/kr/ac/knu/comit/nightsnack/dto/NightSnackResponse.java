package kr.ac.knu.comit.nightsnack.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackStatus;

public record NightSnackResponse(
        Long nightSnackId,
        LocalDate nightSnackDate,
        NightSnackStatus status,
        int capacity,
        int remaining,
        int reservedCapacity,
        int reservedRemaining,
        LocalDateTime openAt,
        LocalDateTime closeAt,
        String title,
        String contents,
        String menu,
        String pickupLocation,
        LocalDateTime pickupStartTime,
        LocalDateTime pickupDeadline,
        boolean requiresStudentCouncilFee
) {
    public static NightSnackResponse from(NightSnack nightSnack) {
        return new NightSnackResponse(
                nightSnack.getId(),
                nightSnack.getNightSnackDate(),
                nightSnack.getStatus(),
                nightSnack.getCapacity(),
                nightSnack.getRemaining(),
                nightSnack.getReservedCapacity(),
                nightSnack.getReservedRemaining(),
                nightSnack.getPeriod().getOpenAt(),
                nightSnack.getPeriod().getCloseAt(),
                nightSnack.getTitle(),
                nightSnack.getContents(),
                nightSnack.getMenu(),
                nightSnack.getPickupLocation(),
                nightSnack.getPickupStartTime(),
                nightSnack.getPickupDeadline(),
                nightSnack.isRequiresStudentCouncilFee()
        );
    }
}
