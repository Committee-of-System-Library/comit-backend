package kr.ac.knu.comit.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Member")
class MemberTest {

    @Test
    @DisplayName("미래 시각까지 정지하면 상태와 판정이 일치한다")
    void suspendsUntilFutureTime() {
        // given
        // 미래 시각까지의 정지 요청을 준비한다.
        Member member = member();
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
        Member member = member();
        LocalDateTime suspendedUntil = LocalDateTime.now().minusDays(1);

        // when & then
        // 과거 시각 정지는 도메인 규칙 위반으로 거부되어야 한다.
        assertThatThrownBy(() -> member.suspend(suspendedUntil))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("회원 삭제 시 개인정보를 비식별화하고 표시 닉네임은 탈퇴한 사용자로 바꾼다")
    void anonymizesPersonalFieldsWhenDeleted() {
        // given
        // 삭제 대상 회원을 준비하고 영속 ID를 부여한다.
        Member member = member();
        ReflectionTestUtils.setField(member, "id", 1L);

        // when
        // 회원 삭제를 수행한다.
        member.delete();

        // then
        // 계정은 soft delete 되고 표시 닉네임과 개인정보가 정책에 맞게 정리되어야 한다.
        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDisplayNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(member.getNickname()).startsWith("deleted-member-");
        assertThat(member.getName()).isEqualTo("탈퇴한 사용자");
        assertThat(member.getPhone()).startsWith("deleted-phone-");
        assertThat(member.getStudentNumber()).isNull();
        assertThat(member.getProfileImageUrl()).isNull();
        assertThat(member.getMajorTrack()).isNull();
        assertThat(member.isStudentNumberVisible()).isFalse();
    }

    private Member member() {
        return Member.create(
                "sso-1",
                "테스트유저",
                "010-0000-0000",
                "comit-user",
                "20230001",
                null,
                null,
                LocalDateTime.now()
        );
    }
}
