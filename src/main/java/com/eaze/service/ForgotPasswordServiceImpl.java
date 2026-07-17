package com.eaze.service;

import com.eaze.domian.VerificationType;
import com.eaze.model.ForgotPasswordToken;
import com.eaze.model.User;
import com.eaze.repository.ForgotPasswordRepository;
import com.eaze.service.domain.ForgotPasswordService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final ForgotPasswordRepository forgotPasswordRepository;

    public ForgotPasswordServiceImpl(ForgotPasswordRepository forgotPasswordRepository) {
        this.forgotPasswordRepository = forgotPasswordRepository;
    }

    @Override
    public ForgotPasswordToken createToken(User user, String id, String otp, VerificationType verificationType, String sendTo) {

        ForgotPasswordToken token = new ForgotPasswordToken();

        token.setId(id);
        token.setUser(user);
        token.setOtp(otp);
        token.setVerificationType(verificationType);
        token.setSendTo(sendTo);

        return forgotPasswordRepository.save(token);
    }

    @Override
    public ForgotPasswordToken findById(String id) {
        Optional<ForgotPasswordToken> token = forgotPasswordRepository.findById(id);
        return token.orElse(null);
    }

    @Override
    public ForgotPasswordToken findByUser(Long id) {
        return forgotPasswordRepository.findByUserId(id);
    }

    @Override
    public void deleteToken(ForgotPasswordToken token) {
        forgotPasswordRepository.delete(token);
    }
}
