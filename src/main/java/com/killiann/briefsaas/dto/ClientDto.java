package com.killiann.briefsaas.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ClientDto {
    private Long id;
    private String name;
    private String email;
}