package kr.ac.knu.comit.main.service;

import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostImage;
import kr.ac.knu.comit.post.domain.PostImageRepository;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.dto.PostSummaryResponse;
import kr.ac.knu.comit.post.service.ContentPreviewGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MainPageQueryService {

    private static final int DEFAULT_SECTION_LIMIT = 5;
    private static final int COMPACT_SECTION_LIMIT = 3;
    private static final List<BoardType> SECTION_ORDER = List.of(
            BoardType.QNA,
            BoardType.INFO,
            BoardType.FREE,
            BoardType.NOTICE,
            BoardType.EVENT
    );
    private static final Map<BoardType, Integer> SECTION_LIMITS = Map.of(
            BoardType.QNA, DEFAULT_SECTION_LIMIT,
            BoardType.INFO, DEFAULT_SECTION_LIMIT,
            BoardType.FREE, DEFAULT_SECTION_LIMIT,
            BoardType.NOTICE, COMPACT_SECTION_LIMIT,
            BoardType.EVENT, COMPACT_SECTION_LIMIT
    );

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentQueryService commentQueryService;
    private final ContentPreviewGenerator contentPreviewGenerator;

    /**
     * 메인 페이지 섹션별 최신 게시글 묶음을 조회한다.
     */
    public MainPageSections getSections(int size) {
        validateSize(size);

        Map<BoardType, List<Post>> sectionPosts = loadSectionPosts(size);
        List<Long> sectionPostIds = sectionPosts.values().stream()
                .flatMap(List::stream)
                .map(Post::getId)
                .toList();

        Map<Long, Post> detailedPostsById = loadDetailedPostsById(sectionPostIds);
        Map<Long, Integer> commentCounts = commentQueryService.countActiveCommentsByPostIds(sectionPostIds);
        Map<Long, List<String>> imageUrlsByPostId = imageUrlsByPostId(sectionPostIds);

        return new MainPageSections(
                toSummaries(sectionPosts.get(BoardType.QNA), detailedPostsById, commentCounts, imageUrlsByPostId),
                toSummaries(sectionPosts.get(BoardType.INFO), detailedPostsById, commentCounts, imageUrlsByPostId),
                toSummaries(sectionPosts.get(BoardType.FREE), detailedPostsById, commentCounts, imageUrlsByPostId),
                toSummaries(sectionPosts.get(BoardType.NOTICE), detailedPostsById, commentCounts, imageUrlsByPostId),
                toSummaries(sectionPosts.get(BoardType.EVENT), detailedPostsById, commentCounts, imageUrlsByPostId)
        );
    }

    private Map<BoardType, List<Post>> loadSectionPosts(int requestedSize) {
        Map<BoardType, List<Post>> postsByBoard = new EnumMap<>(BoardType.class);
        for (BoardType boardType : SECTION_ORDER) {
            int pageSize = Math.min(requestedSize, sectionLimit(boardType));
            postsByBoard.put(boardType, postRepository.findFirstPage(boardType, PageRequest.of(0, pageSize)));
        }
        return postsByBoard;
    }

    private Map<Long, Post> loadDetailedPostsById(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return postRepository.findActiveWithMemberAndTagsByIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, post -> post));
    }

    private Map<Long, List<String>> imageUrlsByPostId(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return postImageRepository.findByPost_IdInOrderBySortOrderAsc(postIds).stream()
                .collect(Collectors.groupingBy(
                        PostImage::getPostId,
                        Collectors.mapping(PostImage::getImageUrl, Collectors.toList())
                ));
    }

    private List<PostSummaryResponse> toSummaries(List<Post> posts,
                                                  Map<Long, Post> detailedPostsById,
                                                  Map<Long, Integer> commentCounts,
                                                  Map<Long, List<String>> imageUrlsByPostId) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        return posts.stream()
                .map(post -> detailedPostsById.get(post.getId()))
                .filter(Objects::nonNull)
                .map(post -> PostSummaryResponse.from(
                        post,
                        commentCounts.getOrDefault(post.getId(), 0),
                        imageUrlsByPostId.getOrDefault(post.getId(), List.of()),
                        contentPreviewGenerator.generate(post.getContent())
                ))
                .toList();
    }

    private void validateSize(int size) {
        if (size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private int sectionLimit(BoardType boardType) {
        return SECTION_LIMITS.getOrDefault(boardType, DEFAULT_SECTION_LIMIT);
    }
}
