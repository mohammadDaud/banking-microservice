package com.bank.dtos;

import com.bank.enums.AuditAction;
import com.bank.enums.AuditModule;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AuditLogSearchRequest {

    private String userId;

    private String username;

    private AuditModule module;

    private AuditAction action;

    private String entityId;

    private String entityType;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Integer page = 0;

    private Integer size = 10;

    private String sortBy = "createdAt";

    private String sortDirection = "DESC";
}