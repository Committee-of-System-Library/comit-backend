package kr.ac.knu.comit.post.domain;

import java.util.List;

public interface PostLikeRepositoryCustom {

    List<PostLike> findMyLikes(Long memberId, Long cursorId, int limit);

    long countMyLikes(Long memberId);
}
