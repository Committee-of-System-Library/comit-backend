package kr.ac.knu.comit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;
import kr.ac.knu.comit.auth.dto.RegisterPrefillResponse;
import kr.ac.knu.comit.auth.dto.RegisterProfileImagePresignedRequest;
import kr.ac.knu.comit.auth.dto.RegisterRequest;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.image.dto.PresignedUploadResponse;
import kr.ac.knu.comit.image.service.ImageService;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterService")
class RegisterServiceTest {

    @Mock
    private ExternalAuthClient externalAuthClient;

    @Mock
    private ExternalIdentityMapper externalIdentityMapper;

    @Mock
    private MemberService memberService;

    @Mock
    private MemberRegistrationService memberRegistrationService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private RegisterService registerService;

    @Test
    @DisplayName("prefill 조회는 JWT claim의 name, studentNumber, major를 반환한다")
    void returnsPrefillFromVerifiedIdentity() {
        ExternalIdentity identity = identity();
        given(externalAuthClient.verify("token-123")).willReturn(identity);
        given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal());
        given(memberService.hasActiveMember("sso-sub-1")).willReturn(false);

        RegisterPrefillResponse response = registerService.getPrefill("token-123");

        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.studentNumber()).isEqualTo("2023012780");
        assertThat(response.major()).isEqualTo("심화");
    }

    @Test
    @DisplayName("이미 가입된 회원이 prefill을 조회하면 MEMBER_ALREADY_EXISTS를 반환한다")
    void throwsWhenPrefillRequestedByExistingMember() {
        ExternalIdentity identity = identity();
        given(externalAuthClient.verify("token-123")).willReturn(identity);
        given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal());
        given(memberService.hasActiveMember("sso-sub-1")).willReturn(true);

        assertThatThrownBy(() -> registerService.getPrefill("token-123"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("회원가입은 JWT claim과 요청 본문으로 MemberRegistrationService를 호출한다")
    void registersUsingVerifiedIdentityAndRequestBody() {
        ExternalIdentity identity = identity();
        given(externalAuthClient.verify("token-123")).willReturn(identity);
        given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal());
        given(memberService.hasActiveMember("sso-sub-1")).willReturn(false);

        registerService.register("token-123", new RegisterRequest("길동이", "010-1234-5678", null, true));

        then(memberRegistrationService).should().register(
                "sso-sub-1",
                "홍길동",
                "010-1234-5678",
                "길동이",
                "2023012780",
                "심화",
                null
        );
    }

    @Test
    @DisplayName("토큰이 없으면 UNAUTHORIZED를 반환한다")
    void throwsUnauthorizedWhenTokenIsMissing() {
        assertThatThrownBy(() -> registerService.getPrefill(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("약관 동의가 false면 INVALID_REQUEST를 반환한다")
    void throwsWhenTermsAreNotAgreed() {
        assertThatThrownBy(() -> registerService.register(
                "token-123",
                new RegisterRequest("길동이", "010-1234-5678", null, false)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("탈퇴한 회원은 다시 가입할 수 있다")
    void allowsReRegistrationAfterWithdrawal() {
        ExternalIdentity identity = identity();
        given(externalAuthClient.verify("token-123")).willReturn(identity);
        given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal());
        given(memberService.hasActiveMember("sso-sub-1")).willReturn(false);

        registerService.register("token-123", new RegisterRequest("길동이", "010-1234-5678", null, true));

        then(memberRegistrationService).should().register(
                "sso-sub-1",
                "홍길동",
                "010-1234-5678",
                "길동이",
                "2023012780",
                "심화",
                null
        );
    }

    @Test
    @DisplayName("이미 가입된 회원이 다시 register를 호출하면 MEMBER_ALREADY_EXISTS를 반환한다")
    void throwsWhenRegisterRequestedByExistingMember() {
        ExternalIdentity identity = identity();
        given(externalAuthClient.verify("token-123")).willReturn(identity);
        given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal());
        given(memberService.hasActiveMember("sso-sub-1")).willReturn(true);

        assertThatThrownBy(() -> registerService.register(
                "token-123",
                new RegisterRequest("길동이", "010-1234-5678", null, true)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("회원가입 전용 프로필 이미지 presigned URL 발급은 가입 전 사용자에게만 허용된다")
    void createsProfileImagePresignedUploadForPendingRegistration() {
        ExternalIdentity identity = identity();
        given(externalAuthClient.verify("token-123")).willReturn(identity);
        given(externalIdentityMapper.toPrincipal(identity)).willReturn(principal());
        given(memberService.hasActiveMember("sso-sub-1")).willReturn(false);
        given(imageService.generatePresignedUrl(any())).willReturn(
                new PresignedUploadResponse(
                        "https://bucket.s3.ap-northeast-2.amazonaws.com/members/profile.png?signature=test",
                        "https://bucket.s3.ap-northeast-2.amazonaws.com/members/profile.png"
                )
        );

        PresignedUploadResponse response = registerService.createProfileImagePresignedUpload(
                "token-123",
                new RegisterProfileImagePresignedRequest("profile.png", "image/png")
        );

        assertThat(response.imageUrl()).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/members/profile.png");
        assertThat(response.presignedUrl()).contains("signature=test");
        then(imageService).should().generatePresignedUrl(any());
    }

    @Test
    @DisplayName("RegisterService에는 트랜잭션 경계를 두지 않는다")
    void doesNotDeclareTransactionBoundary() throws NoSuchMethodException {
        // given
        // 회원 등록은 REQUIRES_NEW인 MemberRegistrationService에 위임된다.
        // 여기에 트랜잭션이 있으면 바깥 트랜잭션이 커넥션을 붙잡은 채 중단되어
        // 가입 1건이 커넥션 2개를 점유한다(pool 8 → 동시 8건이면 connection-timeout).
        // 배경은 docs/ops/transaction-boundary-convention.md L1 참고.

        // when & then
        // 클래스에도 register 메서드에도 @Transactional이 없어야 한다.
        assertThat(RegisterService.class.getAnnotation(Transactional.class))
                .as("RegisterService는 파사드이므로 클래스 레벨 트랜잭션을 갖지 않는다")
                .isNull();
        assertThat(RegisterService.class
                .getDeclaredMethod("register", String.class, RegisterRequest.class)
                .getAnnotation(Transactional.class))
                .as("register()는 REQUIRES_NEW 호출을 감싸므로 트랜잭션을 갖지 않는다")
                .isNull();
    }

    private ExternalIdentity identity() {
        return new ExternalIdentity(
                "sso-sub-1",
                "홍길동",
                "hong@knu.ac.kr",
                "2023012780",
                "심화",
                "CSE_STUDENT",
                "STUDENT"
        );
    }

    private MemberPrincipal principal() {
        return new MemberPrincipal(
                null,
                "sso-sub-1",
                "홍길동",
                "hong@knu.ac.kr",
                "2023012780",
                MemberPrincipal.UserType.CSE_STUDENT,
                MemberPrincipal.MemberRole.STUDENT
        );
    }

}
