package kr.ac.knu.comit.comment.domain;

import java.util.List;

public interface CommentRepositoryCustom {

    List<Comment> findMyComments(Long memberId, Long cursorId, int limit);

    long countMyComments(Long memberId);
}
