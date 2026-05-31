package kr.ac.knu.comit.nightsnack.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트 전용 컨트롤러. {@code comit.load-test.enabled=true} 일 때만 활성화.
 *
 * <p>목적: decrementRemaining 원자적 UPDATE가 대규모 동시 요청에서 정확히 동작하는지 측정한다.
 * member/auth를 완전히 제거해 순수 DB 동시성만 테스트한다.
 */
@ConditionalOnProperty("comit.load-test.enabled")
@RestController
@RequestMapping("/load-test")
@RequiredArgsConstructor
public class LoadTestNightSnackController {

    /** 신청 객체의 고유 학번 생성용. (night_snack_id, student_number) 유니크를 통과시킨다. */
    private static final AtomicLong SEQ = new AtomicLong();

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;

    /**
     * 야식 마차 생성 + 오픈. K6 setup()에서 1번 호출한다.
     * member 시딩 없음 — apply()가 DB 신청 기록 없이 decrementRemaining만 호출하므로 불필요.
     */
    @PostMapping("/setup")
    @Transactional
    public ResponseEntity<ApiResponse<SetupResult>> setup(
            @RequestParam(defaultValue = "100") int capacity
    ) {
        Period period = Period.of(
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2)
        );
        NightSnack nightSnack = NightSnack.create(LocalDate.now(), capacity, period);
        nightSnackRepository.save(nightSnack);
        nightSnack.open();

        return ResponseEntity.ok(ApiResponse.success(new SetupResult(nightSnack.getId())));
    }

    /**
     * 부하 테스트 신청. 실제 신청 플로우처럼 decrementRemaining 원자적 UPDATE + NightSnackApplication
     * INSERT를 같은 트랜잭션에서 수행한다(슬롯 선점과 신청 기록 INSERT 부하를 함께 측정).
     * member/auth는 생략하고, 신청 객체는 회원 없이 고유 학번만으로 만든다.
     * K6 VU가 그냥 POST 요청만 보내면 된다 — 헤더/바디 불필요.
     */
    @PostMapping("/night-snacks/{nightSnackId}/apply")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> apply(@PathVariable Long nightSnackId) {
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId)
                .orElseThrow(() -> new BusinessException(NightSnackErrorCode.EVENT_NOT_FOUND));

        int updated = nightSnackRepository.decrementRemaining(nightSnackId);
        if (updated == 0) {
            if (!nightSnack.isOpen()) {
                throw new BusinessException(NightSnackErrorCode.EVENT_NOT_OPEN);
            }
            throw new BusinessException(NightSnackErrorCode.EVENT_SOLD_OUT);
        }

        // 실제 신청처럼 application 기록을 INSERT (decrement와 같은 트랜잭션 → 실패 시 슬롯 복구)
        String studentNumber = "lt" + SEQ.incrementAndGet(); // 고유, VARCHAR(20) 이내
        NightSnackApplication application = NightSnackApplication.reserved(nightSnack, studentNumber);
        nightSnackApplicationRepository.save(application);

        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 테스트 데이터 초기화. 다음 테스트 실행 전 호출한다. */
    @PostMapping("/cleanup/{nightSnackId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> cleanup(@PathVariable Long nightSnackId) {
        List<NightSnackApplication> applications =
                nightSnackApplicationRepository.findAll().stream()
                        .filter(a -> a.getNightSnack().getId().equals(nightSnackId))
                        .toList();
        nightSnackApplicationRepository.deleteAll(applications);
        nightSnackRepository.findById(nightSnackId).ifPresent(nightSnackRepository::delete);
        return ResponseEntity.ok(ApiResponse.success());
    }

    public record SetupResult(Long nightSnackId) {}
}
