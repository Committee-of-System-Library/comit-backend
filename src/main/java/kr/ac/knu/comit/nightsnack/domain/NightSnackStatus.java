package kr.ac.knu.comit.nightsnack.domain;

/** 스케줄러가 기간에 따라 오픈하고, 정원 도달·시간 만료 시 마감한다. */
public enum NightSnackStatus {
    SCHEDULED,
    OPEN,
    CLOSED
}
