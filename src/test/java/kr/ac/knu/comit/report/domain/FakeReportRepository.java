package kr.ac.knu.comit.report.domain;

import kr.ac.knu.comit.support.FakeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * ReportRepository 인메모리 구현체.
 * 단순 조건 매칭 쿼리만 구현하고, 페이징 쿼리와 어드민 필터는 미지원.
 * 저장 시 예외 시뮬레이션이 필요하면 {@link #onNextSaveThrow(RuntimeException)}을 사용한다.
 */
public class FakeReportRepository extends FakeRepository<Report> implements ReportRepository {

    private RuntimeException nextSaveException;

    @Override
    protected Long getId(Report report) {
        return report.getId();
    }

    @Override
    public <S extends Report> S save(S entity) {
        if (nextSaveException != null) {
            RuntimeException ex = nextSaveException;
            nextSaveException = null;
            throw ex;
        }
        return super.save(entity);
    }

    /** 다음 save() 호출 시 지정한 예외를 던진다. DB 제약 충돌 시나리오 테스트에 사용. */
    public void onNextSaveThrow(RuntimeException exception) {
        this.nextSaveException = exception;
    }

    @Override
    public boolean existsByReporterIdAndTargetTypeAndTargetId(
            Long reporterId, ReportTargetType targetType, Long targetId) {
        return store.values().stream()
                .anyMatch(r -> r.getReporter().getId().equals(reporterId)
                        && r.getTargetType() == targetType
                        && r.getTargetId().equals(targetId));
    }

    @Override
    public Optional<Report> findByIdAndDeletedAtIsNull(Long reportId) {
        return store.values().stream()
                .filter(r -> r.getId().equals(reportId) && !r.isDeleted())
                .findFirst();
    }

    @Override
    public Page<Report> findAllActiveByFilters(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        throw new UnsupportedOperationException("Fake does not support findAllActiveByFilters");
    }
}
