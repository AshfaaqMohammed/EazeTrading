package com.eaze.model;

import com.eaze.domian.VerificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VerificationCode {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String otp;

    @OneToOne
    private User user;
    private String email;
    private String mobile;

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType;
}
