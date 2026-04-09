package kr.ac.knu.comit.post.domain;

import java.util.List;

public interface PostRepositoryCustom {

    /**
     * 특정 회원이 작성한 게시글을 cursor 기반으로 조회한다.
     *
     * @param memberId 조회할 회원 ID
     * @param cursorId null이면 첫 페이지, 값이 있으면 해당 ID 미만부터 조회
     * @param limit    조회할 최대 행 수 (hasNext 판정을 위해 pageSize + 1 을 전달한다)
     */
    List<Post> findMyPosts(Long memberId, Long cursorId, int limit);

    long countMyPosts(Long memberId);
}
