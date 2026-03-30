package kr.ac.knu.comit.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Member")
class MemberTest {

    @Test
    @DisplayName("미래 시각까지 정지하면 상태와 판정이 일치한다")
    void suspendsUntilFutureTime() {
        // given
        // 미래 시각까지의 정지 요청을 준비한다.
        Member member = Member.create("sso-1", "comit-user", "20230001");
        LocalDateTime suspendedUntil = LocalDateTime.now().plusDays(1);

        // when
        // 회원을 정지시킨다.
        member.suspend(suspendedUntil);

        // then
        // 상태와 정지 판정이 모두 정지로 유지되어야 한다.
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getSuspendedUntil()).isEqualTo(suspendedUntil);
        assertThat(member.isSuspended()).isTrue();
    }

    @Test
    @DisplayName("과거 시각 정지는 INVALID_REQUEST로 거부한다")
    void throwsWhenSuspendedUntilIsInThePast() {
        // given
        // 이미 지난 시각까지의 정지 요청을 준비한다.
        Member member = Member.create("sso-1", "comit-user", "20230001");
        LocalDateTime suspendedUntil = LocalDateTime.now().minusDays(1);

        // when & then
        // 과거 시각 정지는 도메인 규칙 위반으로 거부되어야 한다.
        assertThatThrownBy(() -> member.suspend(suspendedUntil))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }
}
