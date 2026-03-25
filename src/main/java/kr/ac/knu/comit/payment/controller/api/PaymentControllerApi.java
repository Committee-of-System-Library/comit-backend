package kr.ac.knu.comit.payment.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.payment.dto.PaymentConfirmRequest;
import kr.ac.knu.comit.payment.dto.PaymentConfirmResponse;
import kr.ac.knu.comit.payment.dto.PaymentDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
public interface PaymentControllerApi {

    @ApiDoc(
            summary = "결제 조회",
            description = "주문 ID 기준으로 결제 상태를 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "orderId", value = "조회할 주문 ID"),
                    @FieldDesc(name = "includeHistory", value = "거래 이력 포함 여부"),
                    @FieldDesc(name = "status", value = "결제 상태"),
                    @FieldDesc(name = "approvedAt", value = "결제 승인 시각"),
                    @FieldDesc(name = "historyIncluded", value = "거래 이력 포함 여부")
            },
            example = @Example(
                    response = """
                            {
                              "orderId": "ORDER-001",
                              "status": "DONE",
                              "approvedAt": "2024-01-01T12:00:00Z",
                              "historyIncluded": true
                            }
                            """
            )
    )
    @GetMapping("/{orderId}")
    ResponseEntity<PaymentDetailResponse> getPayment(
            @PathVariable("orderId") String orderId,
            @RequestParam(name = "includeHistory", defaultValue = "false") boolean includeHistory
    );

    @ApiDoc(
            summary = "결제 승인",
            description = "결제 승인 요청을 처리하고 승인 결과를 반환합니다.",
            descriptions = {
                    @FieldDesc(name = "orderId", value = "주문 ID"),
                    @FieldDesc(name = "amount", value = "결제 금액 (원 단위)"),
                    @FieldDesc(name = "paymentKey", value = "토스 결제 고유 키"),
                    @FieldDesc(name = "status", value = "결제 상태"),
                    @FieldDesc(name = "approvedAt", value = "결제 승인 시각")
            },
            example = @Example(
                    request = """
                            {
                              "orderId": "ORDER-001",
                              "amount": 15000,
                              "paymentKey": "pay_123456789"
                            }
                            """,
                    response = """
                            {
                              "status": "DONE",
                              "approvedAt": "2024-01-01T12:00:00Z",
                              "orderId": "ORDER-001",
                              "amount": 15000
                            }
                            """
            )
    )
    @PostMapping("/confirm")
    ResponseEntity<PaymentConfirmResponse> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request);
}
