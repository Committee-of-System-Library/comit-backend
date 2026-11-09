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
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class NightSnackApplicationService {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final StudentCouncilFeeRepository studentCouncilFeeRepository;
    private final MemberService memberService;
    private final NightSnackInsertWriter reservationWriter;

    public ApplyResponse apply(Long nightSnackId, Long memberId, String studentNumber) {
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));

        if (nightSnack.isRequiresStudentCouncilFee()
                && !studentCouncilFeeRepository.existsPaidByStudentNumber(studentNumber)) {
            throw new BusinessException(NightSnackErrorCode.STUDENT_COUNCIL_FEE_REQUIRED);
        }

        if (nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(memberId, nightSnackId)) {
            throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
        }
        Member member = memberService.findMemberOrThrow(memberId);

        try {
            NightSnackApplication application = reservationWriter.reserve(member, nightSnackId);
            return ApplyResponse.from(application);
        } catch (DataIntegrityViolationException exception) {
            // 유니크 위반 후 쓰기 트랜잭션이 롤백된 다음 중복 여부를 확인한다.
            if (nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(memberId, nightSnackId)
                    || (member.getStudentNumber() != null && nightSnackApplicationRepository
                    .findByNightSnackIdAndStudentNumber(nightSnackId, member.getStudentNumber()).isPresent())) {
                throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
            }
            throw exception;
        }
    }
}
