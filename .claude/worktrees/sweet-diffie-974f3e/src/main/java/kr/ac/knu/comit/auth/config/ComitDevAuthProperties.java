package kr.ac.knu.comit.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comit.dev.auth")
public class ComitDevAuthProperties {

    private boolean enabled;
    private String cookieSameSite;
}
