package kr.ac.knu.comit.nightsnack.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 사전신청(예약분) 배치 등록 요청. 관리자가 학번 목록을 받아 미리 등록한다(요구사항 4-3).
 *
 * @param studentNumbers 등록할 학번 목록. 이미 등록된 학번은 멱등하게 건너뛴다.
 */
public record ReserveRequest(
        @NotEmpty
        List<String> studentNumbers
) {
}
