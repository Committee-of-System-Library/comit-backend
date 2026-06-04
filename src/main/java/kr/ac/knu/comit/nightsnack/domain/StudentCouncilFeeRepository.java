package kr.ac.knu.comit.nightsnack.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCouncilFeeRepository extends JpaRepository<StudentCouncilFee, Long> {

    boolean existsByMemberIdAndPaidTrue(Long memberId);
}
