package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycStatResponse {

    private String status;

    private Long count;
}