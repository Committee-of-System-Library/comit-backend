package kr.ac.knu.comit.auth.service;

import kr.ac.knu.comit.auth.config.AdminEmailProperties;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalIdentityMapper {

    static final String ADMIN_DISPLAY = "관리자";

    private final AdminEmailProperties adminEmailProperties;

    public MemberPrincipal toPrincipal(ExternalIdentity identity) {
        return toPrincipal(null, identity);
    }

    public MemberPrincipal toPrincipal(Long memberId, ExternalIdentity identity) {
        if (adminEmailProperties.isAdminEmail(identity.email())) {
            return new MemberPrincipal(
                    memberId,
                    requiredString(identity.ssoSub()),
                    ADMIN_DISPLAY,
                    ADMIN_DISPLAY,
                    ADMIN_DISPLAY,
                    MemberPrincipal.UserType.CSE_STUDENT,
                    MemberPrincipal.MemberRole.ADMIN
            );
        }
        return new MemberPrincipal(
                memberId,
                requiredString(identity.ssoSub()),
                requiredString(identity.name()),
                requiredString(identity.email()),
                identity.studentNumber(),
                parseUserType(identity.userType()),
                MemberPrincipal.MemberRole.STUDENT  // SSO role 무시, Comit role은 DB에서 관리
        );
    }

    private MemberPrincipal.UserType parseUserType(String rawValue) {
        try {
            return MemberPrincipal.UserType.valueOf(requiredString(rawValue).toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private String requiredString(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return value;
    }
}
