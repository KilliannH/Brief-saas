package com.killiann.briefsaas.dto;

import com.killiann.briefsaas.entity.BriefStatus;
import lombok.Data;

@Data
public class BriefUpdateRequest {
    private BriefStatus status;
}
