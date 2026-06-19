package com.bank.as.repository;

import com.bank.as.model.entites.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByUserIdOrderByEventTimeDesc(String userId);

    List<AuditLog> findByUsernameOrderByEventTimeDesc(String username);
}