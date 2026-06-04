package kr.ac.knu.comit.nightsnack.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comit.night-snack")
public class NightSnackProperties {

    /** 전체 정원 대비 사전신청 예약분 비율. 기본값 10% (application.yml). */
    private double reservedCapacityRatio;
}
