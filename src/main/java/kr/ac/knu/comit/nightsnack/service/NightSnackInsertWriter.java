package kr.ac.knu.comit.nightsnack.service;

import java.time.Clock;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NightSnackInsertWriter {
    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final Clock clock;

    /** 공유 게이트 확인과 INSERT만 같은 트랜잭션에서 수행한다. 정원 초과 접수는 마감 시 선정한다. */
    @Transactional
    public NightSnackApplication reserve(Member member, Long nightSnackId) {
        NightSnack nightSnack = nightSnackRepository.findForApplication(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        if (!nightSnack.isOpen() || !nightSnack.isOpenAt(LocalDateTime.now(clock))) {
            throw new BusinessException(NightSnackErrorCode.EVENT_NOT_OPEN);
        }
        if (nightSnack.generalCapacity() == 0) {
            throw new BusinessException(NightSnackErrorCode.EVENT_SOLD_OUT);
        }
        return nightSnackApplicationRepository.saveAndFlush(NightSnackApplication.general(member, nightSnack));
    }
}
