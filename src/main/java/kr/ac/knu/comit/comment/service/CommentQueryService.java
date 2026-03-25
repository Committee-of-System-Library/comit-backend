package kr.ac.knu.comit.comment.service;

import java.util.List;
import java.util.Map;
import kr.ac.knu.comit.comment.domain.CommentRepository;
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
}
