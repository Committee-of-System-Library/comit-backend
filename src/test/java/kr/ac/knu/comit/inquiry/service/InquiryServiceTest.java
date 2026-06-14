package kr.ac.knu.comit.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.inquiry.domain.Inquiry;
import kr.ac.knu.comit.inquiry.domain.InquiryRepository;
import kr.ac.knu.comit.inquiry.dto.AdminInquiryPageResponse;
import kr.ac.knu.comit.inquiry.dto.CreateInquiryRequest;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryService")
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private InquiryService inquiryService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("정상 요청이면 문의를 저장한다")
        void savesInquiryWhenRequestIsValid() {
            Member member = MemberFixture.member(1L);
            given(memberService.findMemberOrThrow(1L)).willReturn(member);

            inquiryService.create(1L, new CreateInquiryRequest("제목입니다", "내용입니다"));

            then(inquiryRepository).should().save(any(Inquiry.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외를 던진다")
        void throwsWhenMemberNotFound() {
            given(memberService.findMemberOrThrow(1L))
                    .willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

            assertThatThrownBy(() -> inquiryService.create(1L, new CreateInquiryRequest("제목", "내용")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getInquiries")
    class GetInquiries {

        @Test
        @DisplayName("문의 목록을 페이지로 반환한다")
        void returnsInquiryPage() {
            Member member = MemberFixture.member(1L);
            Inquiry inquiry = Inquiry.create(member, "제목입니다", "내용입니다");
            ReflectionTestUtils.setField(inquiry, "id", 1L);
            PageRequest pageable = PageRequest.of(0, 20);
            given(inquiryRepository.findAllByOrderByCreatedAtDesc(pageable))
                    .willReturn(new PageImpl<>(List.of(inquiry), pageable, 1));

            AdminInquiryPageResponse response = inquiryService.getInquiries(pageable);

            assertThat(response.inquiries()).hasSize(1);
            assertThat(response.inquiries().get(0).title()).isEqualTo("제목입니다");
            assertThat(response.totalElements()).isEqualTo(1);
        }
    }
}
