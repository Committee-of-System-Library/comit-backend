package kr.ac.knu.comit.member.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import kr.ac.knu.comit.member.domain.MemberStatus;

public record AdminMemberStatusRequest(
        @NotNull
        MemberStatus status,
        LocalDateTime suspendedUntil
) {
}
