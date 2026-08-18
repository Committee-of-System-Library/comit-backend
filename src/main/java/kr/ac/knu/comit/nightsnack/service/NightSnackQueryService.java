package kr.ac.knu.comit.nightsnack.service;

import java.time.LocalDate;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.domain.StudentCouncilFeeRepository;
import kr.ac.knu.comit.nightsnack.dto.MyTicketResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.StudentCouncilFeeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NightSnackQueryService {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final StudentCouncilFeeRepository studentCouncilFeeRepository;

    /**
     * 기준일(당일 포함) 이후로 가장 가까운 야식 마차를 조회한다.
     *
     * <p>당일에 야식 마차가 있으면 그것을, 없으면 다음 예정 야식 마차를 반환한다.
     * 앞으로 예정된 야식 마차가 하나도 없을 때만 {@code EVENT_NOT_FOUND}.
     */
    public NightSnackResponse getUpcomingFrom(LocalDate date) {
        NightSnack nightSnack = nightSnackRepository
                .findFirstByNightSnackDateGreaterThanEqualOrderByNightSnackDateAsc(date)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        return NightSnackResponse.from(nightSnack);
    }

    public MyTicketResponse getMyTicket(Long nightSnackId, Long memberId, String name) {
        NightSnackApplication application = nightSnackApplicationRepository
                .findByMemberIdAndNightSnackId(memberId, nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.APPLICATION_NOT_FOUND));
        return MyTicketResponse.from(application, name);
    }

    public StudentCouncilFeeResponse getStudentCouncilFeeStatus(String studentNumber) {
        return StudentCouncilFeeResponse.of(studentCouncilFeeRepository.existsPaidByStudentNumber(studentNumber));
    }
}
