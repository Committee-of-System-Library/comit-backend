package kr.ac.knu.comit.auth.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comit.auth")
public class AdminEmailProperties {

    /**
     * 관리자 이메일 목록. 해당 이메일로 인증 시 자동 회원가입 후 ADMIN 권한이 부여된다.
     * 환경변수 COMIT_AUTH_ADMIN_EMAILS 에 콤마(,)로 구분하여 입력한다.
     */
    private List<String> adminEmails = List.of();

    public boolean isAdminEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Set<String> normalized = adminEmails.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return normalized.contains(email.strip().toLowerCase());
    }
}
