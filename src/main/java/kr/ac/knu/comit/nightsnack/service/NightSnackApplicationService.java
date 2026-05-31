package kr.ac.knu.comit.nightsnack.service;

import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선착순 신청 유스케이스. Discussion #96 결정(모델 A + 방안 1 DB 원자적 UPDATE)을 구현한다.
 *
 * <p>정합성의 핵심:
 * <ol>
 *   <li>잔여 수량 감소는 {@link NightSnackRepository#decrementRemaining} 단일 원자적 UPDATE로 처리한다.</li>
 *   <li>감소(선점)와 {@link NightSnackApplication} INSERT는 같은 트랜잭션에 있어야 한다. INSERT가 실패해
 *       롤백되면 감소도 함께 롤백되어 슬롯이 자동 복구된다 — 이것이 중복 신청 방어의 보장 수단이다.</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NightSnackApplicationService {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final MemberService memberService;

    @Transactional
    public ApplyResponse apply(Long nightSnackId, Long memberId) {
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        int generalCapacity = nightSnack.generalCapacity(); // 일반분 정원(예약분 제외)

        // 빠른 중복 차단(보장 수단 아님). 알려진 중복에 원자적 UPDATE 슬롯을 낭비하지 않는다.
        if (nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(memberId, nightSnackId)) {
            throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
        }

        // 방안 1: 단일 원자적 UPDATE로 선점. status/remaining을 WHERE에 함께 두므로
        // 0이면 "오픈 전/마감됨" 또는 "정원 마감" 둘 다일 수 있어 재조회로 구분한다.
        int updated = nightSnackRepository.decrementRemaining(nightSnackId);
        if (updated == 0) {
            NightSnack current = nightSnackRepository.findById(nightSnackId)
                    .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
            if (!current.isOpen()) {
                throw new BusinessException(NightSnackErrorCode.EVENT_NOT_OPEN);
            }
            throw new BusinessException(NightSnackErrorCode.EVENT_SOLD_OUT);
        }

        Member member = memberService.findMemberOrThrow(memberId);
        NightSnackApplication application = NightSnackApplication.general(member, nightSnack);

        try {
            nightSnackApplicationRepository.save(application);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateApplication(exception)) {
                // 롤백이 decrementRemaining까지 되돌려 슬롯을 복구한다.
                throw new BusinessException(NightSnackErrorCode.ALREADY_APPLIED);
            }
            throw exception;
        }

        // decrementRemaining(clearAutomatically=true) 이후 재조회라 remaining은 최신값이다.
        int remaining = nightSnackRepository.findById(nightSnackId)
                .map(NightSnack::getRemaining)
                .orElse(0);
        int sequence = generalCapacity - remaining; // 일반분 기준 몇 번째 신청자인지

        return ApplyResponse.of(application, sequence, remaining);
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
