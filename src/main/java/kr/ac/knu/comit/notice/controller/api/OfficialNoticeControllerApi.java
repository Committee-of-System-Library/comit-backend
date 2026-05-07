package kr.ac.knu.comit.notice.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.notice.dto.CreateOfficialNoticeRequest;
import kr.ac.knu.comit.notice.dto.OfficialNoticeListResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeResponse;
import kr.ac.knu.comit.notice.dto.UpdateOfficialNoticeRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
@RequestMapping("/official-notices")
public interface OfficialNoticeControllerApi {

    @ApiDoc(
            summary = "공지사항 목록 조회",
            description = "공지사항을 cursor 기반으로 최신순 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "cursor", value = "이전 응답의 nextCursorId. 첫 페이지는 생략합니다."),
                    @FieldDesc(name = "size", value = "조회할 공지사항 수입니다. 기본값은 20이고 최대 20입니다."),
                    @FieldDesc(name = "notices", value = "공지사항 요약 목록입니다."),
                    @FieldDesc(name = "id", value = "공지사항 ID입니다."),
                    @FieldDesc(name = "title", value = "공지사항 제목입니다."),
                    @FieldDesc(name = "author", value = "작성자 이름입니다. 없으면 null입니다."),
                    @FieldDesc(name = "originalUrl", value = "원본 공지사항 URL입니다. 없으면 null입니다."),
                    @FieldDesc(name = "postedAt", value = "원본 사이트에 게시된 시각입니다. 없으면 null입니다."),
                    @FieldDesc(name = "createdAt", value = "시스템에 등록된 시각입니다."),
                    @FieldDesc(name = "nextCursorId", value = "다음 페이지 조회에 사용할 마지막 공지사항 ID입니다. 마지막 페이지면 null입니다."),
                    @FieldDesc(name = "hasNext", value = "다음 페이지 존재 여부입니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "notices": [
                                  {
                                    "id": 1,
                                    "title": "2026학년도 1학기 수강신청 안내",
                                    "author": "학사지원팀",
                                    "originalUrl": "https://computer.knu.ac.kr/bbs/board.php?tbl=notice&mode=VIEW&num=1",
                                    "postedAt": "2026-01-10T09:00:00",
                                    "createdAt": "2026-01-10T10:00:00"
                                  }
                                ],
                                "nextCursorId": null,
                                "hasNext": false
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<OfficialNoticeListResponse>> getNotices(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    );

    @ApiDoc(
            summary = "공지사항 상세 조회",
            description = "공지사항 하나의 상세 정보를 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "noticeId", value = "조회할 공지사항 ID입니다."),
                    @FieldDesc(name = "id", value = "공지사항 ID입니다."),
                    @FieldDesc(name = "title", value = "공지사항 제목입니다."),
                    @FieldDesc(name = "content", value = "공지사항 본문입니다."),
                    @FieldDesc(name = "author", value = "작성자 이름입니다. 없으면 null입니다."),
                    @FieldDesc(name = "originalUrl", value = "원본 공지사항 URL입니다. 없으면 null입니다."),
                    @FieldDesc(name = "postedAt", value = "원본 사이트에 게시된 시각입니다. 없으면 null입니다."),
                    @FieldDesc(name = "summary", value = "AI가 생성한 요약문입니다. 아직 생성되지 않은 경우 null입니다."),
                    @FieldDesc(name = "createdAt", value = "시스템에 등록된 시각입니다."),
                    @FieldDesc(name = "updatedAt", value = "마지막 수정 시각입니다. 수정 이력이 없으면 null입니다.")
            },
            errors = {
                    @ApiError(code = "NOTICE_NOT_FOUND", when = "조회 대상 공지사항이 없거나 삭제된 상태일 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "id": 1,
                                "title": "2026학년도 1학기 수강신청 안내",
                                "content": "수강신청은 2026년 2월 10일부터 시작합니다. 자세한 일정은 아래를 참고하세요.",
                                "author": "학사지원팀",
                                "originalUrl": "https://computer.knu.ac.kr/bbs/board.php?tbl=notice&mode=VIEW&num=1",
                                "postedAt": "2026-01-10T09:00:00",
                                "summary": null,
                                "createdAt": "2026-01-10T10:00:00",
                                "updatedAt": null
                              }
                            }
                            """
            )
    )
    @GetMapping("/{noticeId}")
    ResponseEntity<ApiResponse<OfficialNoticeResponse>> getNotice(
            @PathVariable Long noticeId
    );

    @ApiDoc(
            summary = "공지사항 등록",
            description = "새 공지사항을 등록합니다. 관리자 권한이 필요합니다.",
            descriptions = {
                    @FieldDesc(name = "title", value = "공지사항 제목입니다. 최대 300자입니다."),
                    @FieldDesc(name = "content", value = "공지사항 본문입니다."),
                    @FieldDesc(name = "author", value = "작성자 이름입니다. 선택 항목입니다."),
                    @FieldDesc(name = "originalUrl", value = "원본 공지사항 URL입니다. 선택 항목입니다."),
                    @FieldDesc(name = "postedAt", value = "원본 게시 시각입니다. 선택 항목입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "INVALID_TITLE", when = "제목이 비어있거나 300자를 초과할 때"),
                    @ApiError(code = "INVALID_CONTENT", when = "본문이 비어 있을 때")
            },
            example = @Example(
                    request = """
                            {
                              "title": "2026학년도 1학기 수강신청 안내",
                              "content": "수강신청은 2026년 2월 10일부터 시작합니다.",
                              "author": "학사지원팀",
                              "originalUrl": "https://computer.knu.ac.kr/bbs/board.php?tbl=notice&mode=VIEW&num=1",
                              "postedAt": "2026-01-10T09:00:00"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": 1
                            }
                            """
            )
    )
    @PostMapping
    ResponseEntity<ApiResponse<Long>> createNotice(
            @RequestBody @Valid CreateOfficialNoticeRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "공지사항 수정",
            description = "기존 공지사항을 수정합니다. 관리자 권한이 필요합니다.",
            descriptions = {
                    @FieldDesc(name = "noticeId", value = "수정할 공지사항 ID입니다."),
                    @FieldDesc(name = "title", value = "수정할 제목입니다. 최대 300자입니다."),
                    @FieldDesc(name = "content", value = "수정할 본문입니다."),
                    @FieldDesc(name = "author", value = "수정할 작성자 이름입니다. 선택 항목입니다."),
                    @FieldDesc(name = "originalUrl", value = "수정할 원본 URL입니다. 선택 항목입니다."),
                    @FieldDesc(name = "postedAt", value = "수정할 원본 게시 시각입니다. 선택 항목입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "NOTICE_NOT_FOUND", when = "수정 대상 공지사항이 없거나 삭제된 상태일 때"),
                    @ApiError(code = "INVALID_TITLE", when = "제목이 비어있거나 300자를 초과할 때"),
                    @ApiError(code = "INVALID_CONTENT", when = "본문이 비어 있을 때")
            },
            example = @Example(
                    request = """
                            {
                              "title": "2026학년도 1학기 수강신청 안내 (수정)",
                              "content": "수강신청 일정이 변경되었습니다.",
                              "author": "학사지원팀",
                              "originalUrl": "https://computer.knu.ac.kr/bbs/board.php?tbl=notice&mode=VIEW&num=1",
                              "postedAt": "2026-01-10T09:00:00"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/{noticeId}")
    ResponseEntity<ApiResponse<Void>> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody @Valid UpdateOfficialNoticeRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "공지사항 삭제",
            description = "공지사항을 삭제 상태로 변경합니다. 관리자 권한이 필요합니다.",
            descriptions = {
                    @FieldDesc(name = "noticeId", value = "삭제할 공지사항 ID입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "NOTICE_NOT_FOUND", when = "삭제 대상 공지사항이 없거나 이미 삭제된 상태일 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @DeleteMapping("/{noticeId}")
    ResponseEntity<ApiResponse<Void>> deleteNotice(
            @PathVariable Long noticeId,
            @AuthenticatedMember MemberPrincipal principal
    );
}
