package kr.ac.knu.comit.comment.service;

import java.util.List;
import java.util.Map;
import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommentErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentRepository commentRepository;

    public Map<Long, Integer> countActiveCommentsByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return commentRepository.countActiveByPostIds(postIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        CommentRepository.CommentCountView::getPostId,
                        row -> Math.toIntExact(row.getCommentCount())
                ));
    }

    public Comment getActiveCommentOrThrow(Long commentId) {
        return commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }
}
