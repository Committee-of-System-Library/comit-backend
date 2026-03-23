package kr.ac.knu.comit.payment.controller;

import kr.ac.knu.comit.payment.controller.api.PaymentControllerApi;
import kr.ac.knu.comit.payment.dto.PaymentConfirmRequest;
import kr.ac.knu.comit.payment.dto.PaymentConfirmResponse;
import kr.ac.knu.comit.payment.dto.PaymentDetailResponse;
import kr.ac.knu.comit.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentControllerApi {

    private final PaymentService paymentService;

    @Override
    public ResponseEntity<PaymentDetailResponse> getPayment(String orderId, boolean includeHistory) {
        return ResponseEntity.ok(paymentService.getPayment(orderId, includeHistory));
    }

    @Override
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirm(request));
    }
}
