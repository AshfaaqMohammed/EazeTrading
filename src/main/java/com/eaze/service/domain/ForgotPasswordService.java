package com.eaze.service.domain;

import com.eaze.domian.VerificationType;
import com.eaze.model.ForgotPasswordToken;
import com.eaze.model.User;

public interface ForgotPasswordService {

    ForgotPasswordToken createToken(User user, String id, String otp, VerificationType verificationType, String sendTo);

    ForgotPasswordToken findById(String id);

    ForgotPasswordToken findByUser(Long id);

    void deleteToken(ForgotPasswordToken token);
}
