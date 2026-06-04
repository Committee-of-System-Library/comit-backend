헤더 검색바에서 전체 게시판 통합 검색 api (GET/posts/search) 연동을 하려고 합니당!

응답 바디 : totalCount, hasNext, nextCursorId, posts (post 타입은 List<PostSummaryResponse>)

쿼리 파라미터 : keyword, boardType, size, cursor

이렇게 요청드립니다! 게시글 목록 조회(GET /posts) api랑 유사한 구조로 맞춰주시면 좋을 듯합니다