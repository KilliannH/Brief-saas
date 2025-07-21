package com.killiann.briefsaas.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
}