package kr.ac.knu.comit.nightsnack.service;

import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ReservationStrategy}의 {@code time-based} 구현체.
 *
 * <p>status 전이(SCHEDULED→OPEN) 없이 {@code openAt ≤ now < closeAt} 시간 범위만으로
 * 신청 가능 여부를 판단한다. 관리자의 수동 open() 호출이나 스케줄러가 불필요하다.
 *
 * <p>원자적 UPDATE({@link NightSnackRepository#decrementRemainingByTime})가 시간 조건을
 * WHERE 절에 포함하므로, 경쟁 조건 없이 슬롯을 선점한다.
 */
@Service
@ConditionalOnProperty(
        name = "comit.nightsnack.reservation-strategy",
        havingValue = "time-based"
)
@RequiredArgsConstructor
public class TimeBasedReservationStrategy implements ReservationStrategy {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;

    @Override
    @Transactional
    public NightSnackApplication reserve(Member member, NightSnack nightSnack) {
        Long nightSnackId = nightSnack.getId();
        LocalDateTime now = LocalDateTime.now();

        int updated = nightSnackRepository.decrementRemainingByTime(nightSnackId, now);
        if (updated == 0) {
            NightSnack current = nightSnackRepository.findById(nightSnackId)
                    .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
            if (!current.getPeriod().hasStarted(now)) {
                throw new BusinessException(NightSnackErrorCode.EVENT_NOT_OPEN);
            }
            if (current.getPeriod().hasEnded(now)) {
                throw new BusinessException(NightSnackErrorCode.EVENT_NOT_OPEN);
            }
            throw new BusinessException(NightSnackErrorCode.EVENT_SOLD_OUT);
        }

        NightSnackApplication application = NightSnackApplication.general(member, nightSnack);
        try {
            nightSnackApplicationRepository.save(application);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateApplication(exception)) {
                throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
            }
            throw exception;
        }
        return application;
    }

    private boolean isDuplicateApplication(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException hibernateException
                    && containsUniqueConstraint(hibernateException.getConstraintName())) {
                return true;
            }
            if (current instanceof java.sql.SQLException sqlException
                    && (sqlException.getErrorCode() == 1062
                    || containsUniqueConstraint(sqlException.getMessage()))) {
                return true;
            }
            if (containsUniqueConstraint(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsUniqueConstraint(String value) {
        return value != null
                && (value.contains("uk_night_snack_application_member_night_snack")
                || value.contains("uk_night_snack_application_night_snack_student_number")
                || value.contains("Duplicate entry"));
    }
}
