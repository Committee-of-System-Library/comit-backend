package kr.ac.knu.comit.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import kr.ac.knu.comit.fixture.OfficialNoticeFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.dto.CreateOfficialNoticeRequest;
import kr.ac.knu.comit.notice.dto.OfficialNoticeListResponse;
import kr.ac.knu.comit.notice.dto.OfficialNoticeResponse;
import kr.ac.knu.comit.notice.dto.UpdateOfficialNoticeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfficialNoticeService")
class OfficialNoticeServiceTest {

    @Mock
    private OfficialNoticeRepository officialNoticeRepository;

    @InjectMocks
    private OfficialNoticeService officialNoticeService;

    @Test
    @DisplayName("cursor 없이 첫 페이지 목록을 조회한다")
    void returnsFirstPageWhenCursorIsNull() {
        // given
        List<OfficialNotice> notices = List.of(
                OfficialNoticeFixture.notice(3L),
                OfficialNoticeFixture.notice(2L),
                OfficialNoticeFixture.notice(1L)
        );
        given(officialNoticeRepository.findFirstPage(any(PageRequest.class))).willReturn(notices);

        // when
        OfficialNoticeListResponse response = officialNoticeService.getNotices(null, 20);

        // then
        assertThat(response.notices()).hasSize(3);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursorId()).isNull();
    }

    @Test
    @DisplayName("다음 페이지가 있으면 nextCursorId와 hasNext=true를 반환한다")
    void returnsNextCursorIdWhenMorePagesExist() {
        // given
        // size=2 요청 시 3개가 조회되면 다음 페이지 존재
        List<OfficialNotice> notices = List.of(
                OfficialNoticeFixture.notice(3L),
                OfficialNoticeFixture.notice(2L),
                OfficialNoticeFixture.notice(1L)  // extra item
        );
        given(officialNoticeRepository.findFirstPage(any(PageRequest.class))).willReturn(notices);

        // when
        OfficialNoticeListResponse response = officialNoticeService.getNotices(null, 2);

        // then
        assertThat(response.notices()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursorId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("존재하는 공지사항 상세를 조회한다")
    void returnsNoticeDetailWhenFound() {
        // given
        OfficialNotice notice = OfficialNoticeFixture.notice(1L, "수강신청 안내");
        given(officialNoticeRepository.findActiveById(1L)).willReturn(Optional.of(notice));

        // when
        OfficialNoticeResponse response = officialNoticeService.getNotice(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("수강신청 안내");
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 조회 시 NOTICE_NOT_FOUND 예외가 발생한다")
    void throwsWhenNoticeNotFound() {
        // given
        given(officialNoticeRepository.findActiveById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> officialNoticeService.getNotice(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    @DisplayName("공지사항을 생성하면 저장된 ID를 반환한다")
    void savesAndReturnsIdOnCreate() {
        // given
        CreateOfficialNoticeRequest request = new CreateOfficialNoticeRequest(
                "신규 공지사항", "본문 내용", "학사지원팀",
                "https://computer.knu.ac.kr/notice/1",
                LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        given(officialNoticeRepository.save(any(OfficialNotice.class))).willAnswer(invocation -> {
            OfficialNotice saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        // when
        Long noticeId = officialNoticeService.createNotice(request);

        // then
        assertThat(noticeId).isEqualTo(100L);

        ArgumentCaptor<OfficialNotice> captor = ArgumentCaptor.forClass(OfficialNotice.class);
        then(officialNoticeRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("신규 공지사항");
    }

    @Test
    @DisplayName("공지사항을 수정한다")
    void updatesNoticeFields() {
        // given
        OfficialNotice notice = OfficialNoticeFixture.notice(1L, "기존 제목");
        given(officialNoticeRepository.findActiveById(1L)).willReturn(Optional.of(notice));
        UpdateOfficialNoticeRequest request = new UpdateOfficialNoticeRequest(
                "수정된 제목", "수정된 본문", null, null, null
        );

        // when
        officialNoticeService.updateNotice(1L, request);

        // then
        assertThat(notice.getTitle()).isEqualTo("수정된 제목");
        assertThat(notice.getContent()).isEqualTo("수정된 본문");
        assertThat(notice.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("공지사항을 소프트 삭제한다")
    void softDeletesNotice() {
        // given
        OfficialNotice notice = OfficialNoticeFixture.notice(1L);
        given(officialNoticeRepository.findActiveById(1L)).willReturn(Optional.of(notice));

        // when
        officialNoticeService.deleteNotice(1L);

        // then
        assertThat(notice.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 공지사항 수정 시 NOTICE_NOT_FOUND 예외가 발생한다")
    void throwsWhenUpdatingDeletedNotice() {
        // given
        given(officialNoticeRepository.findActiveById(1L)).willReturn(Optional.empty());
        UpdateOfficialNoticeRequest request = new UpdateOfficialNoticeRequest(
                "제목", "본문", null, null, null
        );

        // when & then
        assertThatThrownBy(() -> officialNoticeService.updateNotice(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}
