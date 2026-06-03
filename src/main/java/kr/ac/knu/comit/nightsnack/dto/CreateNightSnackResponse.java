package kr.ac.knu.comit.nightsnack.dto;

public record CreateNightSnackResponse(Long nightSnackId) {

    public static CreateNightSnackResponse from(Long nightSnackId) {
        return new CreateNightSnackResponse(nightSnackId);
    }
}
