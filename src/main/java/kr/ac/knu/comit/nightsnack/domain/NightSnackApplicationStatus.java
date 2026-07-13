package kr.ac.knu.comit.nightsnack.domain;

/**
 * 신청 건의 수령 상태.
 *
 * <p>초안에서는 {@link #PENDING} 만 실제로 생성된다. {@link #CONFIRMED}(QR 스캔 수령) 와
 * {@link #NO_SHOW}(18:30 이후 자동 만료) 는 다음 단계(스캔/노쇼 처리)에서 사용하기 위해
 * 미리 모델링만 해 둔다.
 *
 * <p>{@link #AVAILABLE} 은 {@code skip-locked} 전략 전용 상태다. 좌석을 미리 생성해 둘 때만 쓰이며,
 * {@code FOR UPDATE SKIP LOCKED} 로 선점되면 {@link #PENDING} 으로 전이한다.
 */
public enum NightSnackApplicationStatus {
    AVAILABLE,
    PENDING,
    CONFIRMED,
    NO_SHOW
}
