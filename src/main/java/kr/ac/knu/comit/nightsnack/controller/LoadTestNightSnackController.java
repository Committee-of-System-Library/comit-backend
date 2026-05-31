package kr.ac.knu.comit.nightsnack.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트 전용 Mock 컨트롤러. {@code comit.load-test.enabled=true} 일 때만 활성화.
 *
 * <p><b>staging 환경 전용</b> — production 배포 시에는 절대 활성화하지 않는다.
 *
 * <p>엔드포인트:
 * <ul>
 *   <li>{@code POST /load-test/setup} — 테스트용 야식 마차 생성 + 회원 시드 → nightSnackId/memberIds 반환</li>
 *   <li>{@code POST /load-test/night-snacks/{nightSnackId}/apply} — 인증 우회 신청 (X-Load-Test-Member-Id 헤더)</li>
 * </ul>
 */
@ConditionalOnProperty("comit.load-test.enabled")
@RestController
@RequestMapping("/load-test")
@RequiredArgsConstructor
public class LoadTestNightSnackController {

    private static final AtomicLong MEMBER_SEQ = new AtomicLong(System.currentTimeMillis());

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final NightSnackApplicationService nightSnackApplicationService;
    private final MemberRepository memberRepository;

    /**
     * 테스트 픽스처 생성. K6 setup() 단계에서 1번 호출한다.
     *
     * @param capacity      야식 마차 전체 정원 (기본 100)
     * @param memberCount   시드할 테스트 회원 수 (기본 500 — capacity 초과 요청을 재현하기 위해 넉넉하게)
     * @return nightSnackId와 memberIds 목록
     */
    @PostMapping("/setup")
    @Transactional
    public ResponseEntity<ApiResponse<SetupResult>> setup(
            @RequestParam(defaultValue = "100") int capacity,
            @RequestParam(defaultValue = "500") int memberCount
    ) {
        Period period = Period.of(
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2)
        );
        NightSnack nightSnack = NightSnack.create(LocalDate.now(), capacity, period);
        nightSnackRepository.save(nightSnack);
        nightSnack.open();

        List<Long> memberIds = new ArrayList<>(memberCount);
        for (int i = 0; i < memberCount; i++) {
            long seq = MEMBER_SEQ.getAndIncrement();
            Member member = Member.create(
                    "load-test-sso-" + seq,
                    "부하테스트",
                    "010-0000-0000",
                    "lt" + seq,
                    String.format("9999%06d", seq % 1_000_000),
                    null,
                    null,
                    LocalDateTime.now()
            );
            memberIds.add(memberRepository.save(member).getId());
        }

        return ResponseEntity.ok(ApiResponse.success(new SetupResult(nightSnack.getId(), memberIds)));
    }

    /**
     * 인증을 우회한 신청. X-Load-Test-Member-Id 헤더에 회원 ID를 넣으면 된다.
     * K6의 400명 동시 스파이크 대상 엔드포인트.
     */
    /**
     * 인증을 우회한 신청. X-Load-Test-Member-Id 헤더에 회원 ID를 넣으면 된다.
     * BusinessException(409 sold-out 등)은 전역 핸들러가 처리하므로 K6에서 상태 코드로 구분 가능.
     */
    @PostMapping("/night-snacks/{nightSnackId}/apply")
    public ResponseEntity<ApiResponse<ApplyResponse>> apply(
            @PathVariable Long nightSnackId,
            @RequestHeader("X-Load-Test-Member-Id") Long memberId
    ) {
        ApplyResponse response = nightSnackApplicationService.apply(nightSnackId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 테스트 데이터 초기화. 다음 테스트 실행 전 호출하면 이전 신청을 모두 지운다.
     */
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

    public record SetupResult(Long nightSnackId, List<Long> memberIds) {}
}
