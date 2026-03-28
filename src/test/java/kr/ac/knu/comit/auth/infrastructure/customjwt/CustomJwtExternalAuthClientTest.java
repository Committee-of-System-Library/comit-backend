package kr.ac.knu.comit.auth.infrastructure.customjwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.port.ExternalIdentity;
import kr.ac.knu.comit.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CustomJwtExternalAuthClient")
class CustomJwtExternalAuthClientTest {

    private static final String CLIENT_ID = "cse-a1b2c3d4";
    private static final String CLIENT_SECRET = "01234567890123456789012345678901";
    private static final String ISSUER = "https://chcse.knu.ac.kr/appfn/api";

    private final CustomJwtExternalAuthClient externalAuthClient = new CustomJwtExternalAuthClient(properties());

    @Test
    @DisplayName("유효한 custom JWT면 Comit이 사용할 외부 사용자 모델로 변환한다")
    void returnsClaimsWhenTokenIsValid() throws Exception {
        // given
        // auth-server가 발급한 형식의 유효한 custom JWT를 준비한다.
        String token = signedToken(CLIENT_ID, ISSUER, Date.from(Instant.now().plusSeconds(3600)));

        // when
        // JWT를 검증하고 Comit 내부 claim 형태로 변환한다.
        ExternalIdentity identity = externalAuthClient.verify(token);

        // then
        // ssoSub, userType, role이 외부 사용자 모델 규칙에 맞게 매핑되어야 한다.
        assertThat(identity.ssoSub()).isEqualTo("7");
        assertThat(identity.email()).isEqualTo("hong@knu.ac.kr");
        assertThat(identity.userType()).isEqualTo("CSE_STUDENT");
        assertThat(identity.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("audience가 다르면 UNAUTHORIZED 예외를 던진다")
    void throwsWhenAudienceDoesNotMatch() throws Exception {
        // given
        // 다른 client id를 audience로 가진 토큰을 준비한다.
        String token = signedToken("other-client", ISSUER, Date.from(Instant.now().plusSeconds(3600)));

        // when & then
        // Comit에 발급된 토큰이 아니면 인증에 실패해야 한다.
        assertThatThrownBy(() -> externalAuthClient.verify(token))
                .isInstanceOf(BusinessException.class)
                .hasMessage("인증이 필요합니다.");
    }

    private ComitSsoProperties properties() {
        ComitSsoProperties properties = new ComitSsoProperties();
        properties.setClientId(CLIENT_ID);
        properties.setClientSecret(CLIENT_SECRET);
        properties.setIssuer(ISSUER);
        return properties;
    }

    private String signedToken(String audience, String issuer, Date expiration) throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("7")
                .issuer(issuer)
                .audience(audience)
                .claim("student_number", "2023012780")
                .claim("name", "홍길동")
                .claim("email", "hong@knu.ac.kr")
                .claim("major", "심화컴퓨팅 전공")
                .claim("user_type", "CSE_STUDENT")
                .claim("role", "ADMIN")
                .issueTime(new Date())
                .expirationTime(expiration)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(new MACSigner(CLIENT_SECRET));
        return signedJWT.serialize();
    }
}
