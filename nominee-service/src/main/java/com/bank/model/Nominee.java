package com.bank.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nominee")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nominee {

    @Id
    private String id;

    private String customerId;

    private String accountNumber;

    private String nomineeName;

    private String relationship;

    private LocalDate dateOfBirth;

    private String mobileNumber;

    private String email;

    private String address;

    private Integer percentageShare;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}