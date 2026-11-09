package kr.ac.knu.comit.nightsnack.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;
import kr.ac.knu.comit.nightsnack.domain.NightSnackSelectionStatus;

public record MyTicketResponse(
        String name,
        Long applicationId,
        String ticketToken,
        NightSnackApplicationStatus status,
        NightSnackSelectionStatus selectionStatus,
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
                application.getAvailableTicketToken(),
                application.getStatus(),
                application.getSelectionStatus(),
                application.getNightSnack().getNightSnackDate(),
                application.getNightSnack().getMenu(),
                application.getNightSnack().getPickupLocation(),
                application.getNightSnack().getPickupDeadline(),
                application.getCreatedAt(),
                application.getConfirmedAt()
        );
    }
}
