package com.killiann.briefsaas.dto;

import com.killiann.briefsaas.entity.BriefStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BriefResponse {
    private Long id;
    private UUID publicUuid;
    private String title;
    private String description;
    private List<String> objectives;
    private String targetAudience;
    private String budget;
    private LocalDateTime deadline;
    private List<String> deliverables;
    private String constraints;
    private String clientName;
    private String clientEmail;
    private Boolean clientValidated;
    private LocalDateTime validatedAt;
    private BriefStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}