package kr.ac.knu.comit.payment.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.ac.knu.comit.payment.dto.PaymentConfirmRequest;
import kr.ac.knu.comit.payment.dto.PaymentConfirmResponse;
import kr.ac.knu.comit.payment.dto.PaymentDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 문서 예시 엔드포인트에서 사용하는 파일럿 결제 서비스.
 *
 * @implNote 승인된 결제는 메모리에 유지한다. 외부 저장소 없이도 후속 조회
 * 엔드포인트에서 직전에 승인된 결과를 그대로 보여주기 위한 구현이다.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final Clock clock = Clock.systemUTC();
    private final Map<String, PaymentConfirmResponse> confirmedPayments = new ConcurrentHashMap<>();

    @Transactional
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
