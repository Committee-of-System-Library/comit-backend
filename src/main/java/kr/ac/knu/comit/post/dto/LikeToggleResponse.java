package kr.ac.knu.comit.post.dto;

public record LikeToggleResponse(boolean liked) {

    public static LikeToggleResponse likedState() {
        return new LikeToggleResponse(true);
    }

    public static LikeToggleResponse unlikedState() {
        return new LikeToggleResponse(false);
    }
}
