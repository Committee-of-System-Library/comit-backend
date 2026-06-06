package kr.ac.knu.comit.nightsnack.dto;

import java.util.Optional;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;

public record ApplicationCheckResponse(
        boolean applied,
        NightSnackApplicationSource source,
        NightSnackApplicationStatus status,
        String ticketToken
) {

    public static ApplicationCheckResponse of(Optional<NightSnackApplication> application) {
        return application
                .map(a -> new ApplicationCheckResponse(true, a.getSource(), a.getStatus(), a.getTicketToken()))
                .orElseGet(() -> new ApplicationCheckResponse(false, null, null, null));
    }
}
