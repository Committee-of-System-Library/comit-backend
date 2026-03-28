package kr.ac.knu.comit.auth.infrastructure.customjwt;

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
import kr.ac.knu.comit.auth.port.ExternalAuthClient;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class CustomJwtExternalAuthClient implements ExternalAuthClient {

    private final ComitSsoProperties ssoProperties;

    @Override
    public String buildLoginRedirectUrl(String state) {
        validateRequiredProperties();
        return UriComponentsBuilder.fromUriString(normalizeBaseUrl(ssoProperties.getAuthServerBaseUrl()) + "/login")
                .queryParam("client_id", ssoProperties.getClientId())
                .queryParam("redirect_uri", ssoProperties.getRedirectUri())
                .queryParam("state", state)
                .build(true)
                .toUriString();
    }

    @Override
    public ExternalIdentity verify(String token) {
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

            return new ExternalIdentity(
                    requiredString(claimsSet.getSubject()),
                    requiredString(claimsSet.getStringClaim("name")),
                    requiredString(claimsSet.getStringClaim("email")),
                    claimsSet.getStringClaim("student_number"),
                    requiredString(claimsSet.getStringClaim("user_type")),
                    claimsSet.getStringClaim("role")
            );
        } catch (ParseException | JOSEException exception) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateRequiredProperties() {
        if (isBlank(ssoProperties.getClientId())
                || isBlank(ssoProperties.getClientSecret())
                || isBlank(ssoProperties.getRedirectUri())
                || isBlank(ssoProperties.getIssuer())) {
            throw new BusinessException("SSO 설정이 올바르지 않습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
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

    private String normalizeBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) {
            throw new BusinessException("SSO 인증 서버 주소가 비어 있습니다.", CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
