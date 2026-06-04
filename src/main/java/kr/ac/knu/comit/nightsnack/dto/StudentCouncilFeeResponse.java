package kr.ac.knu.comit.nightsnack.dto;

public record StudentCouncilFeeResponse(boolean paid) {

    public static StudentCouncilFeeResponse of(boolean paid) {
        return new StudentCouncilFeeResponse(paid);
    }
}
