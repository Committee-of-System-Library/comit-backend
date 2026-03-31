package kr.ac.knu.comit.auth.dto;

public record RegisterPrefillResponse(
        String name,
        String studentNumber,
        String major
) {
}
