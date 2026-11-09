package kr.ac.knu.comit.nightsnack.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource;
import kr.ac.knu.comit.nightsnack.domain.NightSnackStatus;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 신청 공유 잠금이 모두 해제된 뒤 마감과 최종 선정을 한 트랜잭션으로 커밋한다. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NightSnackLifecycleService {
    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository applicationRepository;
    private final Clock clock;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void refresh(Long nightSnackId) {
        NightSnack nightSnack = findForUpdate(nightSnackId);
        if (nightSnack.getStatus() == NightSnackStatus.CLOSED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (nightSnack.isScheduled() && nightSnack.getPeriod().hasStarted(now)) {
            nightSnack.open(applicationRepository.nextId());
        }
        if (nightSnack.isOpen() && (nightSnack.getPeriod().hasEnded(now)
                || applicationRepository.countByNightSnackIdAndSource(nightSnackId, NightSnackApplicationSource.GENERAL)
                >= nightSnack.generalCapacity())) {
            selectAndClose(nightSnack);
        }
    }

    @Transactional
    public void open(Long nightSnackId) {
        NightSnack nightSnack = findForUpdate(nightSnackId);
        nightSnack.open(applicationRepository.nextId());
    }

    private void selectAndClose(NightSnack nightSnack) {
        List<Long> candidates = nightSnack.generalCapacity() == 0 ? List.of()
                : applicationRepository.findGeneralCandidateIds(nightSnack.getId(),
                        PageRequest.of(0, nightSnack.generalCapacity()));
        long lastAcceptedId = candidates.isEmpty() ? 0L : candidates.getLast();
        applicationRepository.finalizeSelections(nightSnack.getId(), lastAcceptedId);
        nightSnack.close();
    }

    private NightSnack findForUpdate(Long id) {
        return nightSnackRepository.findForUpdate(id)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
    }
}
