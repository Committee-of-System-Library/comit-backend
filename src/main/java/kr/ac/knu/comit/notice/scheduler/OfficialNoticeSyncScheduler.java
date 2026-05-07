package kr.ac.knu.comit.notice.scheduler;

import kr.ac.knu.comit.notice.service.OfficialNoticeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfficialNoticeSyncScheduler {

    private final OfficialNoticeSyncService syncService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncService.syncInitial();
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void scheduledSync() {
        syncService.syncLatest();
    }
}
