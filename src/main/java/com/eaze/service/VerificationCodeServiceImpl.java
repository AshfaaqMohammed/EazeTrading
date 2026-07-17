package com.eaze.service;

import com.eaze.domian.VerificationType;
import com.eaze.model.User;
import com.eaze.model.VerificationCode;
import com.eaze.repository.VerificationCodeRepository;
import com.eaze.service.domain.VerificationCodeService;
import com.eaze.utils.OtpUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    private final VerificationCodeRepository verificationCodeRepository;

    public VerificationCodeServiceImpl(VerificationCodeRepository verificationCodeRepository) {
        this.verificationCodeRepository = verificationCodeRepository;
    }

    @Override
    public VerificationCode sendVerificationCode(User user, VerificationType verificationType) {
        VerificationCode newVerificationCode = new VerificationCode();
        newVerificationCode.setOtp(OtpUtils.generateOTP());
        newVerificationCode.setVerificationType(verificationType);
        newVerificationCode.setUser(user);

        return verificationCodeRepository.save(newVerificationCode);
    }

    @Override
    public VerificationCode getVerificationCodeById(Long id) throws Exception {
        Optional<VerificationCode> verificationCode = verificationCodeRepository.findById(id);
        if (verificationCode.isPresent()){
            return verificationCode.get();
        }
        throw new Exception("verification code not found");
    }

    @Override
    public VerificationCode getVerificationCodeByUser(Long userId) {
        return verificationCodeRepository.findByUserId(userId);
    }

    @Override
    public void deleteVerificationCode(VerificationCode verificationCode) {
        verificationCodeRepository.delete(verificationCode);
    }
}
