package kr.ac.knu.comit.payment.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.ac.knu.comit.payment.dto.PaymentConfirmRequest;
import kr.ac.knu.comit.payment.dto.PaymentConfirmResponse;
import kr.ac.knu.comit.payment.dto.PaymentDetailResponse;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final Clock clock = Clock.systemUTC();
    // Pilot controller state: confirmed payments are kept in memory so the
    // follow-up GET endpoint can reflect a previously confirmed result.
    private final Map<String, PaymentConfirmResponse> confirmedPayments = new ConcurrentHashMap<>();

    public PaymentConfirmResponse confirm(PaymentConfirmRequest request) {
        PaymentConfirmResponse response = new PaymentConfirmResponse(
                "DONE",
                OffsetDateTime.now(clock),
                request.getOrderId(),
                request.getAmount()
        );
        confirmedPayments.put(request.getOrderId(), response);
        return response;
    }

    public PaymentDetailResponse getPayment(String orderId, boolean includeHistory) {
        PaymentConfirmResponse confirmedPayment = confirmedPayments.get(orderId);
        if (confirmedPayment == null) {
            return new PaymentDetailResponse(
                    orderId,
                    "PENDING",
                    null,
                    includeHistory
            );
        }

        return new PaymentDetailResponse(
                confirmedPayment.getOrderId(),
                confirmedPayment.getStatus(),
                confirmedPayment.getApprovedAt(),
                includeHistory
        );
    }
}
