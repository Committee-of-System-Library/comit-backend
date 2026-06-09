package kr.ac.knu.comit.inquiry.service;

import kr.ac.knu.comit.inquiry.domain.Inquiry;
import kr.ac.knu.comit.inquiry.domain.InquiryRepository;
import kr.ac.knu.comit.inquiry.dto.AdminInquiryPageResponse;
import kr.ac.knu.comit.inquiry.dto.CreateInquiryRequest;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberService memberService;

    @Transactional
    public void create(Long memberId, CreateInquiryRequest request) {
        Member member = memberService.findMemberOrThrow(memberId);
        inquiryRepository.save(Inquiry.create(member, request.title(), request.content()));
    }

    public AdminInquiryPageResponse getInquiries(Pageable pageable) {
        return AdminInquiryPageResponse.from(inquiryRepository.findAllByOrderByCreatedAtDesc(pageable));
    }
}
