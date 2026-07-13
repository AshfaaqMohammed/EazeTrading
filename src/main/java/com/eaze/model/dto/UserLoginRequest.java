package com.eaze.model.dto;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String email;
    private String password;
}
