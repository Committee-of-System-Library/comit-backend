package kr.ac.knu.comit.payment.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(kr.ac.knu.comit.payment.service.PaymentService.class)
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mapsPathVariableAndRequestParamUsingInterfaceAnnotations() throws Exception {
        mockMvc.perform(post("/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "ORDER-001",
                                  "amount": 15000,
                                  "paymentKey": "pay_123456789"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/payments/ORDER-001")
                        .param("includeHistory", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.historyIncluded").value(true))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.approvedAt").value(endsWith("Z")));
    }

    @Test
    void mapsRequestUsingInterfaceAnnotations() throws Exception {
        mockMvc.perform(post("/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "ORDER-001",
                                  "amount": 15000,
                                  "paymentKey": "pay_123456789"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.approvedAt").value(endsWith("Z")))
                .andExpect(jsonPath("$.orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.amount").value(15000));
    }

    @Test
    void validatesRequestBodyUsingInterfaceAnnotations() throws Exception {
        mockMvc.perform(post("/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "",
                                  "amount": 15000,
                                  "paymentKey": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
