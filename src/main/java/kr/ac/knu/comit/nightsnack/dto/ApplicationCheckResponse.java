package kr.ac.knu.comit.nightsnack.dto;

import java.util.Optional;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;
import kr.ac.knu.comit.nightsnack.domain.NightSnackSelectionStatus;

public record ApplicationCheckResponse(
        boolean applied,
        NightSnackApplicationSource source,
        NightSnackApplicationStatus status,
        NightSnackSelectionStatus selectionStatus,
        String ticketToken
) {

    public static ApplicationCheckResponse of(Optional<NightSnackApplication> application, boolean appliedAfterDeadline) {
        return application
                .map(a -> new ApplicationCheckResponse(a.getSelectionStatus() == NightSnackSelectionStatus.ACCEPTED,
                        a.getSource(), a.getStatus(), a.getSelectionStatus(), a.getAvailableTicketToken()))
                .orElseGet(() -> new ApplicationCheckResponse(appliedAfterDeadline, null, null, null, null));
    }
}
