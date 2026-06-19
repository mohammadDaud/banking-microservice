package com.bank.as.repository;

import com.bank.as.model.entites.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository
        extends JpaRepository<LoginAudit, String> {
}
