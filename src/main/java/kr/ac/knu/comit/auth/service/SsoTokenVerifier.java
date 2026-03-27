package kr.ac.knu.comit.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.dto.SsoClaims;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SsoTokenVerifier {

    private final ComitSsoProperties ssoProperties;

    public SsoClaims verify(String token) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(signedJwt.getHeader().getAlgorithm())) {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
            }

            JWSVerifier verifier = new MACVerifier(ssoProperties.getClientSecret().getBytes(StandardCharsets.UTF_8));
            if (!signedJwt.verify(verifier)) {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
            }

            JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();
            validateIssuer(claimsSet.getIssuer());
            validateAudience(claimsSet.getAudience());
            validateExpiration(claimsSet.getExpirationTime());

            return new SsoClaims(
                    requiredString(claimsSet.getSubject()),
                    requiredString(claimsSet.getStringClaim("name")),
                    requiredString(claimsSet.getStringClaim("email")),
                    claimsSet.getStringClaim("student_number"),
                    parseUserType(claimsSet.getStringClaim("user_type")),
                    parseRole(claimsSet.getStringClaim("role"))
            );
        } catch (ParseException | JOSEException exception) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateIssuer(String issuer) {
        if (!requiredString(ssoProperties.getIssuer()).equals(issuer)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateAudience(List<String> audience) {
        if (audience == null || !audience.contains(requiredString(ssoProperties.getClientId()))) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateExpiration(Date expiration) {
        if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private String requiredString(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return value;
    }

    private MemberPrincipal.UserType parseUserType(String rawValue) {
        try {
            return MemberPrincipal.UserType.valueOf(requiredString(rawValue).toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private MemberPrincipal.MemberRole parseRole(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MemberPrincipal.MemberRole.STUDENT;
        }
        return "ADMIN".equalsIgnoreCase(rawValue)
                ? MemberPrincipal.MemberRole.ADMIN
                : MemberPrincipal.MemberRole.STUDENT;
    }
}
