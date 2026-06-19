package com.bank.us.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nominees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nominee {

    @Id
    private String id;

    private String userId;

    private String nomineeName;

    private String relationship;

    private String mobileNumber;

    private Double percentageShare;
}