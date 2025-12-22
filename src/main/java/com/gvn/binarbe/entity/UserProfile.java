package com.gvn.binarbe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * UserProfile entity containing additional customer information.
 * Required fields must be completed before submitting loan applications.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private LocalDate birthdate;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 16)
    private String nik; // Indonesian National ID

    /**
     * Check if profile is complete for loan application submission.
     * All fields (birthdate, phone, address, nik) must be filled.
     */
    public boolean isComplete() {
        return birthdate != null
                && phone != null && !phone.isBlank()
                && address != null && !address.isBlank()
                && nik != null && !nik.isBlank();
    }
}
