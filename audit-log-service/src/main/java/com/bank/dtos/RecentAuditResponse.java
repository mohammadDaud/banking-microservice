package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentAuditResponse {

    private String username;

    private String module;

    private String action;

    private String description;

    private LocalDateTime createdAt;

}