package com.killiann.briefsaas.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BriefRequest {
    private String title;
    private String description;
    private List<String> objectives;
    private String targetAudience;
    private String budget;
    private LocalDateTime deadline;
    private List<String> deliverables;
    private String constraints;
    private String clientName;
}