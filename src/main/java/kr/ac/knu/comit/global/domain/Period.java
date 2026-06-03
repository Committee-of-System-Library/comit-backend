package kr.ac.knu.comit.global.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;

/**
 * 시작~종료 시각을 표현하는 공통 값 객체. {@code close_at > open_at} 이 불변식이다.
 *
 * <p>현재 야식 마차({@code NightSnack})에 임베딩하며,
 * 스케줄러/시간 기반 오픈·마감 판단의 기준 데이터로 쓰인다.
 */
@Embeddable
public class Period {

    @Column(name = "open_at", nullable = false)
    private LocalDateTime openAt;

    @Column(name = "close_at", nullable = false)
    private LocalDateTime closeAt;

    protected Period() {
    }

    /**
     * @param openAt  시작 시각 (null 불가)
     * @param closeAt 종료 시각 (openAt 이후여야 한다)
     */
    public static Period of(LocalDateTime openAt, LocalDateTime closeAt) {
        if (openAt == null || closeAt == null || !closeAt.isAfter(openAt)) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        Period period = new Period();
        period.openAt = openAt;
        period.closeAt = closeAt;
        return period;
    }

    /** 주어진 시각이 이 기간 안에 있는지([openAt, closeAt)). */
    public boolean contains(LocalDateTime dateTime) {
        return !dateTime.isBefore(openAt) && dateTime.isBefore(closeAt);
    }

    /** openAt 이 됐는지(시작 여부). */
    public boolean hasStarted(LocalDateTime dateTime) {
        return !dateTime.isBefore(openAt);
    }

    /** closeAt 이 됐는지(종료 여부). */
    public boolean hasEnded(LocalDateTime dateTime) {
        return !dateTime.isBefore(closeAt);
    }

    public LocalDateTime getOpenAt() {
        return openAt;
    }

    public LocalDateTime getCloseAt() {
        return closeAt;
    }
}
