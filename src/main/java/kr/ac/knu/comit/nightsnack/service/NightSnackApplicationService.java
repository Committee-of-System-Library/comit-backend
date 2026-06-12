package kr.ac.knu.comit.nightsnack.service;

import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.domain.StudentCouncilFeeRepository;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NightSnackApplicationService {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final StudentCouncilFeeRepository studentCouncilFeeRepository;
    private final MemberService memberService;
    private final ReservationStrategy reservationStrategy;

    public ApplyResponse apply(Long nightSnackId, Long memberId, String studentNumber) {
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        int generalCapacity = nightSnack.generalCapacity();

        if (nightSnack.isRequiresStudentCouncilFee()
                && !studentCouncilFeeRepository.existsPaidByStudentNumber(studentNumber)) {
            throw new BusinessException(NightSnackErrorCode.STUDENT_COUNCIL_FEE_REQUIRED);
        }

        if (nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(memberId, nightSnackId)) {
            throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
        }
        Member member = memberService.findMemberOrThrow(memberId);

        NightSnackApplication application = reservationStrategy.reserve(member, nightSnack);

        int remaining = nightSnackRepository.findById(nightSnackId)
                .map(NightSnack::getRemaining)
                .orElse(0);
        int sequence = generalCapacity - remaining;

        return ApplyResponse.of(application, sequence, remaining);
    }
}
