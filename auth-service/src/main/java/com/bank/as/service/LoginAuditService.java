package com.bank.as.service;

import com.bank.as.model.entites.LoginAudit;
import com.bank.as.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditRepository repository;

    public void saveAudit(
            String userId,
            String username,
            String ipAddress,
            Boolean success,
            String reason) {

        LoginAudit audit =
                LoginAudit.builder()
                        .userId(userId)
                        .username(username)
                        .ipAddress(ipAddress)
                        .success(success)
                        .reason(reason)
                        .loginTime(LocalDateTime.now())
                        .build();

        repository.save(audit);
    }
}