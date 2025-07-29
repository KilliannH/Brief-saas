package com.killiann.briefsaas.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String profileImage;
    private String language;
}