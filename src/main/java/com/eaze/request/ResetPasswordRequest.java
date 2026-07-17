package com.eaze.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String otp;
    private String newPassword;
}
