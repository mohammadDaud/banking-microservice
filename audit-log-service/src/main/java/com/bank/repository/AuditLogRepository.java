package com.bank.repository;

import com.bank.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface AuditLogRepository extends
        JpaRepository<AuditLog, String>,
        JpaSpecificationExecutor<AuditLog> {

    long countByAction(com.bank.enums.AuditAction action);

    long countByActionAndCreatedAtBetween(
            com.bank.enums.AuditAction action,
            LocalDateTime start,
            LocalDateTime end
    );

    /*=================DASHBOARD=========================*/
    long count();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByModuleAndAction(String module,String action);
}
