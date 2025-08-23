package com.killiann.briefsaas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {
    private String email;
    private String firstname;
    private String lastname;
    private String profileImage;
    private String currentPriceId;
    private Boolean subscriptionActive;
    private Boolean cancelAtPeriodEnd;
    private Instant subscriptionEndAt;
}
