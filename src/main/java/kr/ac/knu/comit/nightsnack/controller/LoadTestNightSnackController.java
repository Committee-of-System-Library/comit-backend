package kr.ac.knu.comit.nightsnack.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
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
 * <p>목적: 실제 {@link NightSnackApplicationService} 플로우로 대규모 동시 요청을 처리해
 * RPS와 동시성 정확성을 측정한다. setup()에서 테스트용 회원 1~memberCount번을 생성하고,
 * apply()에서 memberId를 받아 실제 서비스 로직에 위임한다.
 */
@ConditionalOnProperty("comit.load-test.enabled")
@RestController
@RequestMapping("/load-test")
@RequiredArgsConstructor
public class LoadTestNightSnackController {

    private final NightSnackRepository nightSnackRepository;
    private final NightSnackApplicationRepository nightSnackApplicationRepository;
    private final MemberRepository memberRepository;
    private final NightSnackApplicationService nightSnackApplicationService;

    /**
     * 야식 마차 생성 + 오픈, 테스트용 회원 생성.
     * k6 setup()에서 1번 호출한다.
     *
     * <p>회원은 ssoSub="lt-{i}" 로 find-or-create하므로 반복 setup 시 중복 생성되지 않는다.
     * 반환된 memberIds를 k6 default()에서 __VU 인덱스로 참조해 memberId를 결정한다.
     */
    @PostMapping("/setup")
    @Transactional
    public ResponseEntity<ApiResponse<SetupResult>> setup(
            @RequestParam(defaultValue = "100") int capacity,
            @RequestParam(defaultValue = "400") int memberCount
    ) {
        List<Long> memberIds = new ArrayList<>(memberCount);
        for (int i = 1; i <= memberCount; i++) {
            final int idx = i;
            Member member = memberRepository.findBySsoSubAndDeletedAtIsNull("lt-" + i)
                    .orElseGet(() -> memberRepository.save(Member.create(
                            "lt-" + idx,
                            "테스터" + idx,
                            String.format("010-%04d-%04d", idx, idx),
                            "lt" + idx,
                            String.format("lt%04d", idx),
                            null,
                            null,
                            LocalDateTime.now()
                    )));
            memberIds.add(member.getId());
        }

        LocalDate date = nightSnackRepository.findAll().stream()
                .map(NightSnack::getNightSnackDate)
                .max(LocalDate::compareTo)
                .map(latest -> latest.plusDays(1))
                .orElse(LocalDate.now());

        Period period = Period.of(
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2)
        );
        NightSnack nightSnack = NightSnack.create(date, capacity, period);
        nightSnackRepository.save(nightSnack);
        nightSnack.open();

        return ResponseEntity.ok(ApiResponse.success(new SetupResult(nightSnack.getId(), memberIds)));
    }

    /**
     * 실제 서비스 로직({@link NightSnackApplicationService#apply})으로 처리하는 부하 테스트 신청.
     * k6 VU가 setup()에서 받은 memberId를 쿼리 파라미터로 넘긴다.
     */
    @PostMapping("/night-snacks/{nightSnackId}/apply")
    public ResponseEntity<ApiResponse<ApplyResponse>> apply(
            @PathVariable Long nightSnackId,
            @RequestParam Long memberId
    ) {
        ApplyResponse response = nightSnackApplicationService.apply(nightSnackId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 테스트 데이터 초기화(신청 + 야식마차). 회원은 다음 실행에서 재사용하므로 삭제하지 않는다. */
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
