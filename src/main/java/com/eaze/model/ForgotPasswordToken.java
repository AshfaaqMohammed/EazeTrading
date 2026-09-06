package com.eaze.model;

import com.eaze.domian.VerificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ForgotPasswordToken {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @OneToOne
    private User user;

    private String otp;

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType;
    private String sendTo;
}
