package com.killiann.briefsaas.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstname;
    private String lastname;
    private String profileImage;
}