package kr.ac.knu.comit.auth.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comit.auth.sso")
public class ComitSsoProperties {

    private boolean enabled;
    private String authServerBaseUrl;
    private String clientId;
    private String clientSecret;
    private String issuer;
    private String redirectUri;
    private String frontendSuccessUrl;
    private String frontendRegisterUrl;
    private String frontendErrorUrl;
    private List<String> allowedRedirectUris = new ArrayList<>();
    private String redirectUriCookieName;
    private String tokenCookieName;
    private String stateCookieName;
    private long stateTtlSeconds;
    private long tokenMaxAgeSeconds;
    private String cookiePath;
    private boolean cookieSecure;
    private String cookieSameSite;
}
