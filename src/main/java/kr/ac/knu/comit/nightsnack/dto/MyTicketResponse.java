package kr.ac.knu.comit.nightsnack.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;

public record MyTicketResponse(
        Long applicationId,
        String ticketToken,
        NightSnackApplicationStatus status,
        LocalDate nightSnackDate,
        String menu,
        String pickupLocation,
        LocalDateTime pickupDeadline,
        LocalDateTime appliedAt,
        LocalDateTime confirmedAt
) {
    public static MyTicketResponse from(NightSnackApplication application) {
        return new MyTicketResponse(
                application.getId(),
                application.getTicketToken(),
                application.getStatus(),
                application.getNightSnack().getNightSnackDate(),
                application.getNightSnack().getMenu(),
                application.getNightSnack().getPickupLocation(),
                application.getNightSnack().getPickupDeadline(),
                application.getCreatedAt(),
                application.getConfirmedAt()
        );
    }
}
