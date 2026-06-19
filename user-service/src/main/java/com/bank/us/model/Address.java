package com.bank.us.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    private String id;

    private String userId;

    private String addressType;

    private String line1;

    private String line2;

    private String city;

    private String state;

    private String country;

    private String postalCode;
}