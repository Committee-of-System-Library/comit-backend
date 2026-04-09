package kr.ac.knu.comit.main.service;

import kr.ac.knu.comit.main.dto.MainPageResponse;
import kr.ac.knu.comit.post.dto.HotPostResponse;
import kr.ac.knu.comit.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MainService {

    private static final int MAIN_SECTION_SIZE = 5;

    private final MainPageQueryService mainPageQueryService;
    private final PostService postService;

    /**
     * 메인 페이지에 필요한 섹션과 인기글을 한 번에 조합한다.
     */
    public MainPageResponse getMainPage() {
        MainPageSections sections = mainPageQueryService.getSections(MAIN_SECTION_SIZE);
        List<HotPostResponse> hotPosts = postService.getHotPosts().posts();
        return MainPageResponse.of(sections, hotPosts);
    }
}
