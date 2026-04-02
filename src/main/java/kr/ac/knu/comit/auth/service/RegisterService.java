package kr.ac.knu.comit.auth.service;

import kr.ac.knu.comit.auth.dto.RegisterPrefillResponse;
import kr.ac.knu.comit.auth.dto.RegisterRequest;
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegisterService {

    private final ExternalAuthClient externalAuthClient;
    private final ExternalIdentityMapper externalIdentityMapper;
    private final MemberService memberService;
    private final MemberRegistrationService memberRegistrationService;

    public RegisterPrefillResponse getPrefill(String token) {
        ExternalIdentity identity = verifyRegistrationIdentity(token);
        validateMemberDoesNotExist(identity.ssoSub());
        return new RegisterPrefillResponse(
                identity.name(),
                identity.studentNumber(),
                identity.major()
        );
    }

    @Transactional
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
        if (memberService.hasAnyMember(ssoSub)) {
            throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }
}
