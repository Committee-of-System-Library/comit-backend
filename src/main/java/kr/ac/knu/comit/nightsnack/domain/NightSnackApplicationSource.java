package kr.ac.knu.comit.nightsnack.domain;

/**
 * 신청 건이 어느 정원 풀에서 나왔는지 구분한다(요구사항 4-3).
 *
 * <ul>
 *   <li>{@link #GENERAL} — 17:30 오픈 후 일반분(capacity - reservedCapacity)에서 선착순으로 선점한 신청.</li>
 *   <li>{@link #RESERVED} — 오픈 전 관리자가 학번만 받아 예약분에서 미리 등록한 사전신청.</li>
 * </ul>
 */
public enum NightSnackApplicationSource {
    GENERAL,
    RESERVED
}
