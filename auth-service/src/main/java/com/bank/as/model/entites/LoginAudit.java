package com.bank.as.model.entites;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAudit {

    @Id
    @UuidGenerator
    private String id;

    private String userId;

    private String username;

    private LocalDateTime loginTime;

    private String ipAddress;

    private Boolean success;

    private String reason;
}