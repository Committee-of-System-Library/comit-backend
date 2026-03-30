package kr.ac.knu.comit.member.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.domain.MemberStatus;
import kr.ac.knu.comit.member.dto.AdminMemberPageResponse;
import kr.ac.knu.comit.member.dto.AdminMemberStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberRepository memberRepository;

    public AdminMemberPageResponse getMembers(MemberStatus status, Pageable pageable) {
        Page<Member> memberPage = memberRepository.findAllActiveForAdmin(status, pageable);
        return AdminMemberPageResponse.from(memberPage);
    }

    @Transactional
    public void updateMemberStatus(Long memberId, AdminMemberStatusRequest request) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        switch (request.status()) {
            case ACTIVE -> member.activate();
            case SUSPENDED -> member.suspend(request.suspendedUntil());
            case BANNED -> member.ban();
        }
    }
}
