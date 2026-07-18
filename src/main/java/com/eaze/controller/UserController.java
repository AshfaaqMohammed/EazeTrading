package com.eaze.controller;

import com.eaze.domian.VerificationType;
import com.eaze.model.ForgotPasswordToken;
import com.eaze.model.User;
import com.eaze.model.VerificationCode;
import com.eaze.request.ForgotPasswordTokenRequest;
import com.eaze.request.ResetPasswordRequest;
import com.eaze.response.ApiResponse;
import com.eaze.response.AuthResponse;
import com.eaze.service.EmailService;
import com.eaze.service.domain.ForgotPasswordService;
import com.eaze.service.domain.UserService;
import com.eaze.service.domain.VerificationCodeService;
import com.eaze.utils.OtpUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;
    private final ForgotPasswordService forgotPasswordService;


    public UserController(UserService userService, EmailService emailService, VerificationCodeService verificationCodeService,
                          ForgotPasswordService forgotPasswordService) {
        this.userService = userService;
        this.emailService = emailService;
        this.verificationCodeService = verificationCodeService;
        this.forgotPasswordService = forgotPasswordService;
    }

    @GetMapping("/api/users/profiles")
    public ResponseEntity<User> getUserProfile(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);

        return new ResponseEntity<User>(user,HttpStatus.OK);
    }

    @PostMapping("/api/users/verification/{verificationType}/send-otp")
    public ResponseEntity<String> sendVerificationOtp(@RequestHeader("Authorization") String jwt,
                                                    @PathVariable VerificationType verificationType) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);

        VerificationCode verificationCode = verificationCodeService.getVerificationCodeByUser(user.getId());

        if (verificationCode == null) {
            verificationCode = verificationCodeService.sendVerificationCode(user, verificationType);
        }
        if (verificationType.equals(VerificationType.EMAIL)) {
            emailService.sendVerificationOtpEmail(user.getEmail(), verificationCode.getOtp());
        }

        return new ResponseEntity<>("OTP is sent successfully", HttpStatus.OK);
    }

    @PatchMapping("/api/users/enable-two-factor/verify-otp/{otp}")
    public ResponseEntity<User> enableTwoFactorAuthentication(@RequestHeader("Authorization") String jwt,
                                                              @PathVariable String otp) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);
        VerificationCode verificationCode = verificationCodeService.getVerificationCodeByUser(user.getId());

        String sendTo = verificationCode.getVerificationType().equals(VerificationType.EMAIL) ?
                verificationCode.getEmail() : verificationCode.getMobile();

        boolean isVerified = verificationCode.getOtp().equals(otp);

        if (isVerified) {
            User updatedUser = userService.enableTwoFactorAuthentication(verificationCode.getVerificationType(),user);
            verificationCodeService.deleteVerificationCode(verificationCode);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        }
        throw new Exception("Wrong OTP, Authentication failed!!!!");
    }

    @PostMapping("auth/users/reset-password/send-otp")
    public ResponseEntity<AuthResponse> sendForgotPasswordOtp(
            @RequestBody ForgotPasswordTokenRequest request) throws Exception {

        User user = userService.findUserByEmail(request.getSendTo());
        String otp = OtpUtils.generateOTP();
        String id = UUID.randomUUID().toString();

        ForgotPasswordToken token = forgotPasswordService.findByUser(user.getId());

        if (token == null) {
            token = forgotPasswordService.createToken(user, id, otp, request.getVerificationType(), request.getSendTo());
        }

        if (request.getVerificationType().equals(VerificationType.EMAIL)) {
            emailService.sendVerificationOtpEmail(request.getSendTo(), token.getOtp());
        }

        AuthResponse res = new AuthResponse();
        res.setSession(token.getId());
        res.setMessage(("Password reset otp sent successfully"));

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @PatchMapping("/auth/users/reset-password/verify-otp")
    public ResponseEntity<ApiResponse> resetPassword(
            @RequestParam String id,
            @RequestBody ResetPasswordRequest req) throws Exception {
        ForgotPasswordToken forgotPasswordToken = forgotPasswordService.findById(id);
        boolean isVerified = forgotPasswordToken.getOtp().equals(req.getOtp());

        if (isVerified) {
            userService.updatePassword(forgotPasswordToken.getUser(), req.getNewPassword());
            ApiResponse res = new ApiResponse();
            res.setMessage("Password update successfully");
            return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
        }
        throw new Exception("Wrong Otp, Reset password failed!!!");
    }

}
