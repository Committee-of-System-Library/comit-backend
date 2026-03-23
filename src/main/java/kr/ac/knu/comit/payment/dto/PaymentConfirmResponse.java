package kr.ac.knu.comit.payment.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentConfirmResponse {
    private final String status;
    private final OffsetDateTime approvedAt;
    private final String orderId;
    private final Integer amount;
}
