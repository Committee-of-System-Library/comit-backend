package kr.ac.knu.comit.comment.dto;

public record HelpfulToggleResponse(boolean helpful) {

    public static HelpfulToggleResponse helpfulState() {
        return new HelpfulToggleResponse(true);
    }

    public static HelpfulToggleResponse notHelpfulState() {
        return new HelpfulToggleResponse(false);
    }
}
