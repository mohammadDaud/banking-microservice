package com.bank.acnumgens.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "sequence_master")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SequenceMaster {
    @Id
    @UuidGenerator
    private String sequenceId;

    @Column(unique = true)
    private String sequenceType;

    private long currentValue;
}

