package com.killiann.briefsaas.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BriefResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String clientName;
    private LocalDateTime validatedAt;
    private String publicUuid;
}