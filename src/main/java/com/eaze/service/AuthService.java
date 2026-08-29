package com.eaze.service;

import com.eaze.config.JwtProvider;
import com.eaze.model.TwoFactorOTP;
import com.eaze.model.User;
import com.eaze.request.UserLoginRequest;
import com.eaze.repository.UserRepository;
import com.eaze.response.AuthResponse;
import com.eaze.service.domain.TwoFactorOTPService;
import com.eaze.service.domain.WatchListService;
import com.eaze.utils.OtpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final TwoFactorOTPService twoFactorOTPService;
    private final EmailService emailService;
    private final WatchListService watchListService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;


    public AuthResponse register(User user) throws Exception {

        User isEmailExist = userRepository.findByEmail(user.getEmail());
        if (isEmailExist != null){
            throw new Exception("Email is already used with another account.");
        }

        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setEmail(user.getEmail());

        User savedUser = userRepository.save(newUser);

        watchListService.createWatchList(savedUser);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(savedUser.getEmail());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = jwtProvider.generateToken(auth);

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

        String jwt = jwtProvider.generateToken(auth);

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
        if(!passwordEncoder.matches(password, userDetails.getPassword())){
            throw new BadCredentialsException("invalid password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }



}
