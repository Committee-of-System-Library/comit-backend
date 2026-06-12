package kr.ac.knu.comit.nightsnack.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선착순 선점의 임계구역만 담는 쓰기 트랜잭션 — {@link ReservationStrategy}의 {@code db-atomic-update} 구현체.
 *
 * <p>핫 row 락(remaining 행)은 {@link #reserve}의 {@code decrementRemaining}에서 잡혀 커밋까지 유지되므로,
 * 이 트랜잭션 안에는 (성공 경로 기준) {@code decrement → INSERT} 외 어떤 조회도 두지 않는다.
 *
 * <p>별도 빈으로 둔 이유: {@code @Transactional}은 스프링 프록시로만 적용되어, 같은 클래스 내부 호출
 * (self-invocation)로는 새 트랜잭션 경계가 생기지 않는다. {@link NightSnackApplicationService}가
 * 주입받아 호출해야 이 메서드만의 짧은 트랜잭션이 만들어진다.
 */
@Service
@ConditionalOnProperty(
        name = "comit.nightsnack.reservation-strategy",
        havingValue = "db-atomic-update",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class NightSnackReservationWriter implements ReservationStrategy {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;

    /**
     * 잔여 1 감소(선점)와 신청 INSERT를 한 트랜잭션에 묶는다. INSERT가 유니크 제약으로 실패하면 롤백되어
     * 감소도 되돌아가 슬롯이 자동 복구된다 — 중복 신청 방어의 보장 수단.
     *
     * @param member     읽기 단계에서 이미 로딩된 회원 엔티티. 여기서 studentNumber를 읽어도 추가 SELECT가 없다.
     * @param nightSnack 읽기 단계에서 이미 로딩된 행사 엔티티. 연관 FK 설정에만 쓰인다.
     */
    @Transactional
    public NightSnackApplication reserve(Member member, NightSnack nightSnack) {
        Long nightSnackId = nightSnack.getId();

        int updated = nightSnackRepository.decrementRemaining(nightSnackId);
        if (updated == 0) {
            NightSnack current = nightSnackRepository.findById(nightSnackId)
                    .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
            if (!current.isOpen()) {
                throw new BusinessException(NightSnackErrorCode.EVENT_NOT_OPEN);
            }
            throw new BusinessException(NightSnackErrorCode.EVENT_SOLD_OUT);
        }

        if (nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(member.getId(), nightSnackId)) {
            throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
        }
        NightSnackApplication application = NightSnackApplication.general(member, nightSnack);
        nightSnackApplicationRepository.save(application);
        return application;
    }
}
