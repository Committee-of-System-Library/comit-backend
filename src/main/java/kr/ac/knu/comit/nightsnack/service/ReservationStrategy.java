package kr.ac.knu.comit.nightsnack.service;

import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;

/**
 * 선착순 선점 전략 인터페이스 - Toggle.
 *
 * <p>활성 구현체는 {@code comit.nightsnack.reservation-strategy} 프로퍼티로 선택한다.
 * <ul>
 *   <li>{@code db-atomic-update} (기본): {@link NightSnackReservationWriter} — DB 원자적 UPDATE</li>
 *   <li>{@code time-based}: {@link TimeBasedReservationStrategy} — 시간 범위 기반 DB 원자적 UPDATE</li>
 *   <li>{@code skip-locked}: {@link SkipLockedReservationStrategy} — 좌석 사전 생성 + DB SKIP LOCKED 분산 선점</li>
 *   <li>{@code was-cas}: WAS 메모리 ConcurrentHashMap + AtomicLong CAS (미구현)</li>
 * </ul>
 */
public interface ReservationStrategy {
    NightSnackApplication reserve(Member member, NightSnack nightSnack);

    /**
     * 신청 오픈 전 좌석/슬롯 등 사전 준비가 필요한 전략을 위한 훅. 대부분의 전략(원자적 UPDATE 계열)은
     * 별도 준비가 필요 없어 기본은 no-op이다. {@link SkipLockedReservationStrategy} 만 재정의해
     * {@code generalCapacity()} 개의 빈 좌석을 미리 만든다.
     */
    default void prepareSeats(NightSnack nightSnack) {
    }
}
