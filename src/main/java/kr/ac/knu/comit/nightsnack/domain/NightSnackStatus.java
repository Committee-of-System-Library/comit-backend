package kr.ac.knu.comit.nightsnack.domain;

/**
 * 선착순 이벤트(간식마차/야식마차)의 신청 가능 상태.
 *
 * <p>SCHEDULED → OPEN 전환 트리거(17:30 정각)는 아직 미확정이다.
 * 요구사항 4-2에서는 관리자 토글을 검토 중이며, 스케줄러 도입 여부는 별도 결정이 필요하다.
 */
public enum NightSnackStatus {
    SCHEDULED,
    OPEN,
    CLOSED
}
