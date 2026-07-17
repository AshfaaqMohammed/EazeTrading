package com.eaze.request;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String email;
    private String password;
}
