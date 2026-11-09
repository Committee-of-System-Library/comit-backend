package kr.ac.knu.comit.nightsnack.domain;

/** 수령 상태. PENDING은 미수령, CONFIRMED는 수령 완료이며 신청 당락과 별개다. */
public enum NightSnackApplicationStatus {
    PENDING,
    CONFIRMED,
    NO_SHOW
}
