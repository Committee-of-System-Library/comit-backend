package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.Optional;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.comment.service.CommentService;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.domain.MemberStatus;
import kr.ac.knu.comit.member.dto.AdminMemberStatusRequest;
import kr.ac.knu.comit.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMemberService")
class AdminMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Test
    @DisplayName("미래 시각 정지 요청을 정상적으로 반영한다")
    void suspendsMemberUntilFutureTime() {
        // given
        // 미래 시각까지 정지할 회원을 준비한다.
        Member member = MemberFixture.member(1L, "member-1");
        LocalDateTime suspendedUntil = LocalDateTime.now().plusDays(1);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

        // when
        // 관리자 정지 요청을 처리한다.
        adminMemberService.updateMemberStatus(1L, new AdminMemberStatusRequest(MemberStatus.SUSPENDED, suspendedUntil));

        // then
        // 상태와 정지 시각이 모두 반영되어야 한다.
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getSuspendedUntil()).isEqualTo(suspendedUntil);
        assertThat(member.isSuspended()).isTrue();
    }

    @Test
    @DisplayName("과거 시각 정지 요청은 INVALID_REQUEST로 거부한다")
    void throwsWhenSuspendedUntilIsInThePast() {
        // given
        // 이미 지난 시각까지 정지할 회원을 준비한다.
        Member member = MemberFixture.member(1L, "member-1");
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

        // when & then
        // 과거 시각 정지는 도메인 규칙 위반으로 거부되어야 한다.
        assertThatThrownBy(() ->
                adminMemberService.updateMemberStatus(
                        1L,
                        new AdminMemberStatusRequest(MemberStatus.SUSPENDED, LocalDateTime.now().minusDays(1))
                ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("관리자 삭제 요청은 회원을 소프트 삭제한다")
    void deletesMemberSoftly() {
        // given
        // 삭제 대상 활성 회원을 준비한다.
        Member member = MemberFixture.member(1L, "member-1");
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

        // when
        // 관리자 회원 삭제를 수행한다.
        adminMemberService.deleteMember(1L);

        // then
        // 회원은 소프트 삭제되고 개인정보는 비식별화되어야 한다.
        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDisplayNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(member.getNickname()).startsWith("deleted-member-");
        assertThat(member.getStudentNumber()).isNull();
        assertThat(member.getProfileImageUrl()).isNull();
        assertThat(member.isStudentNumberVisible()).isFalse();
        then(postService).should().removeMemberInteractions(1L);
        then(commentService).should().removeMemberLikes(1L);
    }

    @Test
    @DisplayName("존재하지 않거나 이미 삭제된 회원 삭제 요청은 MEMBER_NOT_FOUND를 던진다")
    void throwsWhenDeletingMissingMember() {
        // given
        // 삭제 대상 회원이 존재하지 않는 상황을 준비한다.
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

        // when & then
        // 관리자 회원 삭제는 MEMBER_NOT_FOUND로 실패해야 한다.
        assertThatThrownBy(() -> adminMemberService.deleteMember(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }
}
