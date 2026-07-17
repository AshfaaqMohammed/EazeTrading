package com.eaze.service;

import com.eaze.config.JwtProvider;
import com.eaze.model.TwoFactorOTP;
import com.eaze.model.User;
import com.eaze.request.UserLoginRequest;
import com.eaze.repository.UserRepository;
import com.eaze.response.AuthResponse;
import com.eaze.service.domain.TwoFactorOTPService;
import com.eaze.utils.OtpUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final TwoFactorOTPService twoFactorOTPService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, CustomUserDetailsService customUserDetailsService,
                       TwoFactorOTPService twoFactorOTPService, EmailService emailService){
        this.userRepository = userRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.twoFactorOTPService = twoFactorOTPService;
        this.emailService = emailService;
    }

    public AuthResponse register(User user) throws Exception {

        User isEmailExist = userRepository.findByEmail(user.getEmail());
        if (isEmailExist != null){
            throw new Exception("Email is already used with another account.");
        }

        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setPassword(user.getPassword());
        newUser.setEmail(user.getEmail());

        userRepository.save(newUser);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                user.getPassword()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        AuthResponse response = new AuthResponse();
        response.setJwt(jwt);
        response.setStatus(true);
        response.setMessage("register success");

        return response;
    }

    public AuthResponse login(UserLoginRequest user) throws Exception {
        String email = user.getEmail();
        String password = user.getPassword();

        User authUser = userRepository.findByEmail(email);

        Authentication auth = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        if (authUser.getTwoFactorAuth().isEnabled()) {
            AuthResponse res = new AuthResponse();
            res.setMessage("Two factor auth is enabled");
            res.setTwoFactorAuthEnabled(true);

            String otp = OtpUtils.generateOTP();

            TwoFactorOTP oldOtp = twoFactorOTPService.findByUser(authUser.getId());
            if (oldOtp != null){
                twoFactorOTPService.deleteTwoFactorOtp(oldOtp);
            }

            TwoFactorOTP newOtp = twoFactorOTPService.createTwoFactorOtp(authUser, otp, jwt);

            emailService.sendVerificationOtpEmail(email, otp);

            res.setSession(newOtp.getId());
            return res;
        }

        AuthResponse res = new AuthResponse();
        res.setJwt(jwt);
        res.setStatus(true);
        res.setMessage("login success");

        return res;

    }

    public AuthResponse verifyLoginOtp(String otp, String id) throws Exception {

        TwoFactorOTP twoFactorOTP = twoFactorOTPService.findById(id);

        if (twoFactorOTPService.verifyTwoFactorOtp(twoFactorOTP, otp)) {
            AuthResponse res = new AuthResponse();
            res.setMessage("Two factor authentication verified");
            res.setTwoFactorAuthEnabled(true);
            res.setJwt(twoFactorOTP.getJwt());
            return res;
        }
        throw new Exception("Invalid otp");
    }

    private Authentication authenticate(String email, String password){
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        if (userDetails == null) {
            throw new BadCredentialsException("invalid username");
        }
        if(!password.equals(userDetails.getPassword())){
            throw new BadCredentialsException("invalid password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }



}
