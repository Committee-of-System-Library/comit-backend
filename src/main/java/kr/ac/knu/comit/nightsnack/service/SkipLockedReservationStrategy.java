package kr.ac.knu.comit.nightsnack.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ReservationStrategy}의 {@code skip-locked} 구현체 — 벤치마크 전용.
 *
 * <p>{@code db-atomic-update}/{@code time-based} 는 {@code night_snack.remaining} 이라는 단일 행을
 * 모든 요청이 함께 두고 경합한다("핫 row"). 이 전략은 {@link #prepareSeats} 로 좌석을
 * {@code night_snack_application} 행으로 미리 만들어 두고, 각 요청이 {@code FOR UPDATE SKIP LOCKED} 로
 * 서로 다른 좌석 행을 잡게 해 행 락 경합 자체를 없앤다. 비교 대상인 {@code time-based} 와 동일하게
 * 오픈 판정은 {@link NightSnack#isOpenAt} 시간 범위로 한다 — 두 전략의 유일한 차이가 "락 전략"이
 * 되도록 다른 변수를 맞춘다.
 *
 * <p><b>벤치마크 전용 범위</b>: {@link kr.ac.knu.comit.nightsnack.dto.NightSnackResponse},
 * {@link NightSnackQueryService} 등 조회 경로는 여전히 {@code NightSnack.remaining} 필드를 읽는데,
 * 이 전략은 그 필드를 갱신하지 않는다. 부하테스트(k6)로 이 전략의 처리량/레이턴시 특성만 검증하는
 * 동안에는 문제가 없지만, 실제 트래픽에 노출하려면 잔여 수량 표시 경로를 좌석 상태 COUNT 기반으로
 * 바꿔야 한다.
 */
@Service
@ConditionalOnProperty(
        name = "comit.nightsnack.reservation-strategy",
        havingValue = "skip-locked"
)
@RequiredArgsConstructor
public class SkipLockedReservationStrategy implements ReservationStrategy {

    private final NightSnackApplicationRepository nightSnackApplicationRepository;

    @Override
    @Transactional
    public NightSnackApplication reserve(Member member, NightSnack nightSnack) {
        Long nightSnackId = nightSnack.getId();

        if (!nightSnack.isOpenAt(LocalDateTime.now())) {
            throw new BusinessException(NightSnackErrorCode.EVENT_NOT_OPEN);
        }

        Long seatId = nightSnackApplicationRepository.findAvailableSeatIdForUpdateSkipLocked(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_SOLD_OUT));

        if (nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(member.getId(), nightSnackId)) {
            throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
        }

        int claimed = nightSnackApplicationRepository.claimSeat(seatId, member.getId(), member.getStudentNumber());
        if (claimed == 0) {
            // 1단계 FOR UPDATE 로 같은 트랜잭션이 이미 X Lock을 쥔 행이라 정상 경로에서는 도달하지 않는다.
            throw new BusinessException(NightSnackErrorCode.EVENT_SOLD_OUT);
        }

        return nightSnackApplicationRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
    }

    /**
     * {@code generalCapacity()} 개의 빈 좌석을 미리 만든다. 신청 오픈 전(관리자 생성 시점 또는 부하테스트
     * setup 시점)에 1회 호출하는 것을 전제로 한다 — 오픈 이후 호출하면 이미 진행 중인 선점과 섞여
     * 좌석 수가 어긋날 수 있다.
     */
    @Override
    @Transactional
    public void prepareSeats(NightSnack nightSnack) {
        int seatCount = nightSnack.generalCapacity();
        List<NightSnackApplication> seats = new ArrayList<>(seatCount);
        for (int i = 0; i < seatCount; i++) {
            seats.add(NightSnackApplication.seat(nightSnack));
        }
        nightSnackApplicationRepository.saveAll(seats);
    }
}
