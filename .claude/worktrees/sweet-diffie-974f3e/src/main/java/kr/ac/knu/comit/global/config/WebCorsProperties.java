package kr.ac.knu.comit.global.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "comit.web.cors")
public class WebCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
