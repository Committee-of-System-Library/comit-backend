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
 *   <li>{@code skip-locked}: DB SKIP LOCKED 슬롯 선점 (미구현)</li>
 *   <li>{@code was-cas}: WAS 메모리 ConcurrentHashMap + AtomicLong CAS (미구현)</li>
 * </ul>
 */
public interface ReservationStrategy {
    NightSnackApplication reserve(Member member, NightSnack nightSnack);
}
