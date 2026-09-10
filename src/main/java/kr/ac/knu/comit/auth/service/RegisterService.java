package kr.ac.knu.comit.auth.service;

import kr.ac.knu.comit.auth.dto.RegisterPrefillResponse;
import kr.ac.knu.comit.auth.dto.RegisterProfileImagePresignedRequest;
import kr.ac.knu.comit.auth.dto.RegisterRequest;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.image.dto.PresignedUploadRequest;
import kr.ac.knu.comit.image.dto.PresignedUploadResponse;
import kr.ac.knu.comit.image.service.ImageService;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SSO 토큰 검증, 회원 등록, 프로필 이미지 presigned URL 발급을 조합하는 회원가입 파사드.
 *
 * @implNote 이 클래스에는 의도적으로 {@code @Transactional}을 두지 않는다. 토큰 검증
 * ({@link ExternalAuthClient})과 presigned URL 생성({@link ImageService})은 DB 작업이 아니고,
 * DB 접근은 {@link MemberService}와 {@link MemberRegistrationService}가 각자의 트랜잭션에서 처리한다.
 *
 * <p>특히 {@link #register}에 트랜잭션을 두면 안 된다. {@link MemberRegistrationService#register}가
 * {@code REQUIRES_NEW}라 바깥 트랜잭션을 중단시키되 그 커넥션은 반납하지 않으므로, 요청 1건이
 * 커넥션 2개를 동시에 점유하게 된다. Hikari pool이 8이라 동시 가입 8건이면 전원이 서로의 커넥션을
 * 기다리다 {@code connection-timeout}으로 실패한다.
 *
 * <p>배경과 규칙은 {@code docs/ops/transaction-boundary-convention.md} 참고.
 */
@Service
@RequiredArgsConstructor
public class RegisterService {

    private final ExternalAuthClient externalAuthClient;
    private final ExternalIdentityMapper externalIdentityMapper;
    private final MemberService memberService;
    private final MemberRegistrationService memberRegistrationService;
    private final ImageService imageService;

    public RegisterPrefillResponse getPrefill(String token) {
        ExternalIdentity identity = verifyRegistrationIdentity(token);
        validateMemberDoesNotExist(identity.ssoSub());
        return new RegisterPrefillResponse(
                identity.name(),
                identity.studentNumber(),
                identity.major()
        );
    }

    /**
     * SSO 토큰의 신원과 요청 본문으로 회원을 등록한다.
     *
     * @implNote {@code validateMemberDoesNotExist}는 사용자에게 빠른 에러를 주기 위한 자문 검사일 뿐이고,
     * 실제 중복 가입 보증은 {@code uk_member_sso_sub} 유니크 제약이다
     * ({@link MemberRegistrationService}에서 {@code MEMBER_ALREADY_EXISTS}로 번역된다).
     * 따라서 이 메서드를 하나의 트랜잭션으로 묶을 이유가 없다.
     */
    public void register(String token, RegisterRequest request) {
        if (!request.agreedToTerms()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        ExternalIdentity identity = verifyRegistrationIdentity(token);
        validateMemberDoesNotExist(identity.ssoSub());

        memberRegistrationService.register(
                identity.ssoSub(),
                identity.name(),
                request.phone(),
                request.nickname(),
                identity.studentNumber(),
                identity.major(),
                request.profileImageUrl()
        );
    }

    public PresignedUploadResponse createProfileImagePresignedUpload(
            String token,
            RegisterProfileImagePresignedRequest request
    ) {
        ExternalIdentity identity = verifyRegistrationIdentity(token);
        validateMemberDoesNotExist(identity.ssoSub());
        return imageService.generatePresignedUrl(
                new PresignedUploadRequest(request.fileName(), request.contentType(), "members")
        );
    }

    private ExternalIdentity verifyRegistrationIdentity(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        ExternalIdentity identity = externalAuthClient.verify(token);
        MemberPrincipal principal = externalIdentityMapper.toPrincipal(identity);
        if (principal.userType() == MemberPrincipal.UserType.EXTERNAL) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return identity;
    }

    private void validateMemberDoesNotExist(String ssoSub) {
        if (memberService.hasActiveMember(ssoSub)) {
            throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }
}
