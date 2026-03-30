package kr.ac.knu.comit.fixture;

import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.report.domain.Report;
import kr.ac.knu.comit.report.domain.ReportStatus;
import kr.ac.knu.comit.report.domain.ReportTargetType;
import org.springframework.test.util.ReflectionTestUtils;

public class ReportFixture {

    public static Report report(Long id, Member reporter, ReportTargetType targetType, Long targetId) {
        Report report = Report.create(reporter, targetType, targetId, "신고 사유입니다");
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    public static Report report(Long id, Member reporter) {
        return report(id, reporter, ReportTargetType.POST, 10L);
    }

    public static Report reviewedReport(Long id, Member reporter, ReportStatus status) {
        Report report = report(id, reporter);
        ReflectionTestUtils.setField(report, "status", status);
        return report;
    }
}
