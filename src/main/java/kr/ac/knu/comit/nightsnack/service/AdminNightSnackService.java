package kr.ac.knu.comit.nightsnack.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.nightsnack.config.NightSnackProperties;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.dto.ApplicationCheckResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.ReserveResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 야식 마차 생성/오픈 및 사전신청(예약분) 등록을 위한 관리자 유스케이스.
 *
 * <p>오픈 트리거(17:30 정각)를 관리자 토글로 둘지 스케줄러로 둘지는 미확정 — 여기서는 수동
 * {@link #open} 만 제공한다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminNightSnackService {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final NightSnackProperties nightSnackProperties;

    @Transactional
    public Long createNightSnack(LocalDate nightSnackDate, int capacity, Period period,
                                 String title, String contents, String menu,
                                 String pickupLocation, LocalDateTime pickupDeadline,
                                 boolean requiresStudentCouncilFee) {
        int reservedCapacity = (int) Math.round(capacity * nightSnackProperties.getReservedCapacityRatio());
        NightSnack nightSnack = NightSnack.create(nightSnackDate, capacity, reservedCapacity, period,
                title, contents, menu, pickupLocation, pickupDeadline, requiresStudentCouncilFee);
        return nightSnackRepository.save(nightSnack).getId();
    }

    @Transactional
    public void open(Long nightSnackId) {
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        nightSnack.open();
    }

    @Transactional
    public void close(Long nightSnackId) {
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        nightSnack.close();
    }

    /**
     * 사전신청(예약분) 배치 등록(요구사항 4-3). 학번만 받아 오픈 전에 미리 신청을 만들어 둔다.
     *
     * <p>멱등하게 동작한다: 이미 신청된(사전신청 + 일반) 학번은 건너뛰고 신규 학번만 예약분에서 차감해
     * 등록한다. 신규 학번 수가 예약 잔여분을 넘기면 전체를 거부한다({@code RESERVED_CAPACITY_EXCEEDED}).
     *
     * <p><b>SCHEDULED(오픈 전) 상태에서만</b> 허용한다(요구사항 "미리 신청해두고"). 예약분 카운터는 엔티티
     * 변경 감지로 감소하는데, {@code NightSnack} 에 {@code @DynamicUpdate} 가 없어 flush 시 모든 컬럼이
     * 다시 쓰인다. 만약 OPEN 중에 예약하면 그 flush가 동시 선착순의 원자적 {@code decrementRemaining} 으로
     * 줄어든 {@code remaining} 을 스냅샷 값으로 덮어써 소진된 슬롯을 되살릴 수 있다(#96 oversell 불변식 위반).
     * SCHEDULED 로 한정하면 {@code decrementRemaining(WHERE status=OPEN)} 이 동시에 일어날 수 없어 이 충돌이 사라진다.
     */
    @Transactional
    public ReserveResponse reserve(LocalDate nightSnackDate, List<String> studentNumbers) {
        NightSnack nightSnack = nightSnackRepository.findByNightSnackDate(nightSnackDate)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        if (!nightSnack.isScheduled()) {
            throw new BusinessException(NightSnackErrorCode.RESERVATION_NOT_ALLOWED);
        }

        Set<String> requested = normalize(studentNumbers);
        Set<String> alreadyRegistered = Set.copyOf(
                nightSnackApplicationRepository.findStudentNumbersByNightSnackId(nightSnack.getId()));

        List<String> toInsert = requested.stream()
                .filter(studentNumber -> !alreadyRegistered.contains(studentNumber))
                .toList();

        if (!toInsert.isEmpty() && !nightSnack.reserve(toInsert.size())) {
            throw new BusinessException(NightSnackErrorCode.RESERVED_CAPACITY_EXCEEDED);
        }

        List<NightSnackApplication> created = new ArrayList<>();
        for (String studentNumber : toInsert) {
            created.add(nightSnackApplicationRepository.save(
                    NightSnackApplication.reserved(nightSnack, studentNumber)));
        }
        return ReserveResponse.of(requested.size(), created);
    }

    public List<NightSnackResponse> listNightSnacks() {
        return nightSnackRepository.findAllByOrderByNightSnackDateDesc().stream()
                .map(NightSnackResponse::from)
                .toList();
    }

    public ApplicationCheckResponse checkApplication(Long nightSnackId, String studentNumber) {
        if (!nightSnackRepository.existsById(nightSnackId)) {
            throw new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND);
        }
        Optional<NightSnackApplication> application =
                nightSnackApplicationRepository.findByNightSnackIdAndStudentNumber(nightSnackId, studentNumber);
        return ApplicationCheckResponse.of(application);
    }

    /** 학번 목록을 정규화한다: 공백 제거, 빈 값 제외, 순서를 유지하며 중복 제거. */
    private Set<String> normalize(List<String> studentNumbers) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String studentNumber : studentNumbers) {
            if (studentNumber == null) {
                continue;
            }
            String trimmed = studentNumber.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }
}
