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

/**
 * 선착순 신청 유스케이스. Discussion #96 결정(모델 A + 방안 1 DB 원자적 UPDATE)을 구현한다.
 *
 * <p>동시성/성능 핵심: 핫 row 락(=remaining 행)은 {@code decrementRemaining}에서 잡혀 커밋까지 유지된다.
 * 그래서 회원 조회·정원 확인·중복 선검사 같은 읽기는 모두 이 단계(트랜잭션 밖)에서 끝내고, 락을 쥐는
 * 쓰기 트랜잭션({@link NightSnackReservationWriter#reserve})에는 {@code decrement → INSERT → commit}
 * 만 담아 임계구역을 최소화한다.
 *
 * <p>이 클래스에는 트랜잭션을 걸지 않는다 — 읽기 쿼리는 Spring Data가 각자 짧게 처리하고, 유일한 쓰기
 * 트랜잭션은 writer 빈에만 둔다. (클래스에 {@code @Transactional}을 걸면 reserve가 그 트랜잭션에 합류해
 * 락이 응답 조립까지 유지되므로 의도적으로 비워 둔다.)
 */
@Service
@RequiredArgsConstructor
public class NightSnackApplicationService {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final StudentCouncilFeeRepository studentCouncilFeeRepository;
    private final MemberService memberService;
    private final NightSnackReservationWriter reservationWriter;

    public ApplyResponse apply(Long nightSnackId, Long memberId) {
        // --- 읽기 단계: 핫 row 락을 잡지 않는다 ---
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        int generalCapacity = nightSnack.generalCapacity(); // 일반분 정원(예약분 제외)

        if (nightSnack.isRequiresStudentCouncilFee()
                && !studentCouncilFeeRepository.existsByMemberIdAndPaidTrue(memberId)) {
            throw new BusinessException(NightSnackErrorCode.STUDENT_COUNCIL_FEE_REQUIRED);
        }

        // 빠른 중복 차단(보장 수단 아님). 알려진 중복에 원자적 UPDATE 슬롯을 낭비하지 않는다.
        if (nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(memberId, nightSnackId)) {
            throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
        }
        // 이미 로딩된 회원 엔티티를 그대로 넘긴다 — 쓰기 트랜잭션(락 구간) 안에서 추가 SELECT가 나지 않도록.
        Member member = memberService.findMemberOrThrow(memberId);

        // --- 쓰기 단계: 락을 쥐는 트랜잭션은 이 호출 하나뿐 ---
        NightSnackApplication application = reservationWriter.reserve(member, nightSnack);

        // --- 응답 조립: sequence 산출용 재조회도 락 밖에서 ---
        int remaining = nightSnackRepository.findById(nightSnackId)
                .map(NightSnack::getRemaining)
                .orElse(0);
        int sequence = generalCapacity - remaining; // 일반분 기준 몇 번째 신청자인지

        return ApplyResponse.of(application, sequence, remaining);
    }
}
