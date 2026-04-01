package kr.ac.knu.comit.member.service;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.dto.MemberProfileResponse;
import kr.ac.knu.comit.member.dto.UpdateNicknameRequest;
import kr.ac.knu.comit.member.dto.UpdateStudentNumberVisibilityRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberProfileResponse getMyProfile(Long memberId) {
        return MemberProfileResponse.from(findMemberOrThrow(memberId));
    }

    @Transactional
    public Optional<Member> findBySso(MemberPrincipal principal) {
        return memberRepository.findBySsoSubAndDeletedAtIsNull(principal.ssoSub())
                .map(member -> syncStudentNumber(member, principal.studentNumber()));
    }

    public boolean hasActiveMember(String ssoSub) {
        return memberRepository.findBySsoSubAndDeletedAtIsNull(ssoSub).isPresent();
    }

    public boolean hasDeletedMember(String ssoSub) {
        return memberRepository.existsBySsoSubAndDeletedAtIsNotNull(ssoSub);
    }

    public boolean hasAnyMember(String ssoSub) {
        return memberRepository.existsBySsoSub(ssoSub);
    }

    @Transactional
    public void updateNickname(Long memberId, UpdateNicknameRequest request) {
        Member member = findMemberOrThrow(memberId);
        if (member.getNickname().equals(request.nickname())) {
            return;
        }
        if (memberRepository.existsByNicknameAndIdNot(request.nickname(), memberId)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }
        member.updateNickname(request.nickname());
    }

    @Transactional
    public void updateStudentNumberVisibility(Long memberId, UpdateStudentNumberVisibilityRequest request) {
        findMemberOrThrow(memberId).updateStudentNumberVisibility(request.visible());
    }

    public Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private Member syncStudentNumber(Member member, String studentNumber) {
        member.syncStudentNumber(studentNumber);
        return member;
    }
}
