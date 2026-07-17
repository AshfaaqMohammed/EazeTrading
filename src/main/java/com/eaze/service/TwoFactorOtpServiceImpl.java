package com.eaze.service;

import com.eaze.model.TwoFactorOTP;
import com.eaze.model.User;
import com.eaze.repository.TwoFactorOtpRepository;
import com.eaze.service.domain.TwoFactorOTPService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TwoFactorOtpServiceImpl implements TwoFactorOTPService {

    private final TwoFactorOtpRepository twoFactorOtpRepository;

    public TwoFactorOtpServiceImpl(TwoFactorOtpRepository twoFactorOtpRepository){
        this.twoFactorOtpRepository = twoFactorOtpRepository;
    }

    @Override
    public TwoFactorOTP createTwoFactorOtp(User user, String otp, String jwt) {
        UUID uuid = UUID.randomUUID();

        String id = uuid.toString();

        TwoFactorOTP newOtp = new TwoFactorOTP();

        newOtp.setId(id);
        newOtp.setOtp(otp);
        newOtp.setJwt(jwt);
        newOtp.setUser(user);

        return twoFactorOtpRepository.save(newOtp);
    }

    @Override
    public TwoFactorOTP findByUser(Long userId) {
        return twoFactorOtpRepository.findByUserId(userId);
    }

    @Override
    public TwoFactorOTP findById(String id) {
        // used Optional because, otp can be null;
        Optional<TwoFactorOTP> otp = twoFactorOtpRepository.findById(id);
        return otp.orElse(null);
    }

    @Override
    public boolean verifyTwoFactorOtp(TwoFactorOTP twoFactorOTP, String otp) {
        return twoFactorOTP.getOtp().equals(otp);
    }

    @Override
    public void deleteTwoFactorOtp(TwoFactorOTP twoFactorOTP) {
        twoFactorOtpRepository.delete(twoFactorOTP);
    }
}
