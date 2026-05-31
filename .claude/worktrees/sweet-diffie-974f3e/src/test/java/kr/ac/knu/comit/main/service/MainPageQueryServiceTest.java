package kr.ac.knu.comit.main.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.PostImageRepository;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.service.ContentPreviewGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainPageQueryService")
class MainPageQueryServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private ContentPreviewGenerator contentPreviewGenerator;

    @InjectMocks
    private MainPageQueryService mainPageQueryService;

    @Test
    @DisplayName("QNA/INFO/FREE는 5개, NOTICE/EVENT는 3개까지 조회한다")
    void usesBoardSpecificSectionLimits() {
        // given
        // 게시판별 조회 결과가 비어 있는 상황을 준비한다.
        given(postRepository.findFirstPage(any(BoardType.class), any(PageRequest.class))).willReturn(java.util.List.of());

        // when
        // 메인 페이지 섹션 조회를 실행한다.
        mainPageQueryService.getSections(5);

        // then
        // 게시판별 정책에 맞는 page size로 첫 페이지 조회가 호출되어야 한다.
        then(postRepository).should().findFirstPage(BoardType.QNA, PageRequest.of(0, 5));
        then(postRepository).should().findFirstPage(BoardType.INFO, PageRequest.of(0, 5));
        then(postRepository).should().findFirstPage(BoardType.FREE, PageRequest.of(0, 5));
        then(postRepository).should().findFirstPage(BoardType.NOTICE, PageRequest.of(0, 3));
        then(postRepository).should().findFirstPage(BoardType.EVENT, PageRequest.of(0, 3));
        then(postRepository).should(never()).findActiveWithMemberAndTagsByIds(org.mockito.ArgumentMatchers.anyList());
        then(commentQueryService).should().countActiveCommentsByPostIds(java.util.List.of());
        verifyNoInteractions(postImageRepository, contentPreviewGenerator);
    }
}
