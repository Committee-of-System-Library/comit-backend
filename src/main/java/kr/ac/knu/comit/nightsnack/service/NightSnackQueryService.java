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

    public NightSnackResponse getByDate(LocalDate date) {
        NightSnack nightSnack = nightSnackRepository.findByNightSnackDate(date)
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
