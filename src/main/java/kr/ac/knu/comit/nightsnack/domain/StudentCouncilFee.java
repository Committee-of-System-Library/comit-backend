package kr.ac.knu.comit.nightsnack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_council_fee")
public class StudentCouncilFee {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "student_number", length = 20, unique = true)
    private String studentNumber;

    @Column(name = "is_paid", nullable = false)
    private boolean paid;

    protected StudentCouncilFee() {
    }
}
