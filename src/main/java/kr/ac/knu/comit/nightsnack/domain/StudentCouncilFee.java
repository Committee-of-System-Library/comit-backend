package kr.ac.knu.comit.nightsnack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_council_fee")
public class StudentCouncilFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_number", nullable = false, length = 20, unique = true)
    private String studentNumber;

    @Column(name = "is_paid", nullable = false)
    private boolean paid;

    protected StudentCouncilFee() {
    }
}
