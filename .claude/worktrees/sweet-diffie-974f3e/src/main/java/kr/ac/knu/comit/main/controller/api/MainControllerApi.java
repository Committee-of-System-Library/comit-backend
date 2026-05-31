package kr.ac.knu.comit.main.controller.api;

import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.main.dto.MainPageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/main")
public interface MainControllerApi {

    @ApiDoc(
            summary = "메인 페이지 조회",
            description = "비로그인 사용자를 포함한 모든 방문자가 메인 페이지에서 QNA, INFO, FREE 최신 게시글 5개와 NOTICE, EVENT 최신 게시글 3개, 인기글을 한 번에 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "qna", value = "QNA 게시판의 최신 5개 게시글 요약 목록입니다."),
                    @FieldDesc(name = "info", value = "INFO 게시판의 최신 5개 게시글 요약 목록입니다."),
                    @FieldDesc(name = "free", value = "FREE 게시판의 최신 5개 게시글 요약 목록입니다."),
                    @FieldDesc(name = "notice", value = "NOTICE 게시판의 최신 3개 게시글 요약 목록입니다."),
                    @FieldDesc(name = "event", value = "EVENT 게시판의 최신 3개 게시글 요약 목록입니다."),
                    @FieldDesc(name = "hotPosts", value = "최근 7일 기준 반응 점수가 높은 인기글 5개 목록입니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "qna": [
                                  {
                                    "id": 101,
                                    "boardType": "QNA",
                                    "title": "JPA fetch join 질문",
                                    "contentPreview": "join fetch와 entity graph 차이가 궁금합니다.",
                                    "authorNickname": "backend-dev",
                                    "likeCount": 3,
                                    "commentCount": 2,
                                    "tags": [
                                      "spring",
                                      "jpa"
                                    ],
                                    "imageUrls": [
                                      "https://cdn.example.com/posts/101-1.png"
                                    ],
                                    "createdAt": "2026-03-24T10:00:00"
                                  }
                                ],
                                "info": [],
                                "free": [],
                                "notice": [
                                  {
                                    "id": 201,
                                    "boardType": "NOTICE",
                                    "title": "개강 안내",
                                    "contentPreview": "2026년 1학기 개강 일정 안내입니다.",
                                    "authorNickname": "admin",
                                    "likeCount": 0,
                                    "commentCount": 0,
                                    "tags": [],
                                    "imageUrls": [],
                                    "createdAt": "2026-03-23T09:00:00"
                                  }
                                ],
                                "event": [],
                                "hotPosts": [
                                  {
                                    "rank": 1,
                                    "id": 101,
                                    "boardType": "QNA",
                                    "title": "JPA fetch join 질문",
                                    "authorNickname": "backend-dev",
                                    "likeCount": 3,
                                    "commentCount": 2,
                                    "tags": [
                                      "spring",
                                      "jpa"
                                    ],
                                    "createdAt": "2026-03-24T10:00:00"
                                  }
                                ]
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<MainPageResponse>> getMainPage();
}
