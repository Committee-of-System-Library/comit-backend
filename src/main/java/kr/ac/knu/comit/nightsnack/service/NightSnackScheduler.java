package kr.ac.knu.comit.nightsnack.service;

import java.time.Clock;
import java.time.LocalDateTime;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "comit.night-snack.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class NightSnackScheduler {
    private final NightSnackRepository nightSnackRepository;
    private final NightSnackLifecycleService lifecycleService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${comit.night-snack.scheduler-delay-ms:1000}")
    public void refresh() {
        for (Long id : nightSnackRepository.findActiveIds(LocalDateTime.now(clock))) {
            try {
                lifecycleService.refresh(id);
            } catch (RuntimeException exception) {
                log.error("야식마차 상태 갱신 실패: nightSnackId={}", id, exception);
            }
        }
    }
}
