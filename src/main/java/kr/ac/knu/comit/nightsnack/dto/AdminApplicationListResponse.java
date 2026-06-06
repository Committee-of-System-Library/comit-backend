package kr.ac.knu.comit.nightsnack.dto;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;

public record AdminApplicationListResponse(
        int total,
        List<ApplicationEntry> applications
) {

    public static AdminApplicationListResponse from(List<NightSnackApplication> applications) {
        List<ApplicationEntry> entries = applications.stream().map(ApplicationEntry::from).toList();
        return new AdminApplicationListResponse(entries.size(), entries);
    }

    public record ApplicationEntry(
            Long applicationId,
            String studentNumber,
            NightSnackApplicationSource source,
            NightSnackApplicationStatus status,
            String ticketToken,
            LocalDateTime createdAt
    ) {
        public static ApplicationEntry from(NightSnackApplication application) {
            return new ApplicationEntry(
                    application.getId(),
                    application.getStudentNumber(),
                    application.getSource(),
                    application.getStatus(),
                    application.getTicketToken(),
                    application.getCreatedAt()
            );
        }
    }
}
