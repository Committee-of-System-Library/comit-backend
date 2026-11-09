package kr.ac.knu.comit.nightsnack.service;

import java.time.Clock;
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
import org.springframework.transaction.annotation.Propagation;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource;

/** 야식마차 생성, 접수 상태 관리와 사전신청 등록. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminNightSnackService {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final NightSnackProperties nightSnackProperties;
    private final Clock clock;
    private final NightSnackLifecycleService lifecycleService;

    @Transactional
    public Long createNightSnack(LocalDate nightSnackDate, int capacity, Period period,
                                 String title, String contents, String menu,
                                 String pickupLocation, LocalDateTime pickupStartTime, LocalDateTime pickupDeadline,
                                 boolean requiresStudentCouncilFee) {
        int reservedCapacity = (int) Math.round(capacity * nightSnackProperties.getReservedCapacityRatio());
        NightSnack nightSnack = NightSnack.create(nightSnackDate, capacity, reservedCapacity, period,
                title, contents, menu, pickupLocation, pickupStartTime, pickupDeadline, requiresStudentCouncilFee);
        return nightSnackRepository.save(nightSnack).getId();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void open(Long nightSnackId) {
        lifecycleService.open(nightSnackId);
    }

    /** 예약분 등록도 행사 배타 잠금으로 오픈·동시 예약과 직렬화한다. */
    @Transactional
    public ReserveResponse reserve(LocalDate nightSnackDate, List<String> studentNumbers) {
        NightSnack nightSnack = nightSnackRepository.findByDateForUpdate(nightSnackDate)
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
                .map(n -> NightSnackResponse.from(n, nightSnackApplicationRepository
                        .countByNightSnackIdAndSource(n.getId(), NightSnackApplicationSource.GENERAL)))
                .toList();
    }

    public ApplicationCheckResponse checkApplication(Long nightSnackId, String studentNumber) {
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));
        Optional<NightSnackApplication> application =
                nightSnackApplicationRepository.findByNightSnackIdAndStudentNumber(nightSnackId, studentNumber);
        return ApplicationCheckResponse.of(application, isPickupDeadlineReached(nightSnack));
    }

    private boolean isPickupDeadlineReached(NightSnack nightSnack) {
        LocalDateTime pickupDeadline = nightSnack.getPickupDeadline();
        return pickupDeadline != null && !LocalDateTime.now(clock).isBefore(pickupDeadline);
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
