package kr.ac.knu.comit.nightsnack.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCouncilFeeRepository extends JpaRepository<StudentCouncilFee, Long> {

    default boolean existsPaidByStudentNumber(String studentNumber) {
        if (studentNumber == null || studentNumber.isBlank()) {
            return false;
        }
        return existsByStudentNumberAndPaidTrue(studentNumber.trim());
    }

    boolean existsByStudentNumberAndPaidTrue(String studentNumber);
}
