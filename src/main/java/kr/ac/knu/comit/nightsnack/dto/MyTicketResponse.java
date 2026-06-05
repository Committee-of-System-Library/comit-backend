package kr.ac.knu.comit.nightsnack.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;

public record MyTicketResponse(
        String name,
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
    public static MyTicketResponse from(NightSnackApplication application, String name) {
        return new MyTicketResponse(
                name,
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
