package com.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "field_operator_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_field_operator_mapping",
                columnNames = {"field_id", "operator_id"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldOperatorMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private FieldMaster field;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private ConditionalOperator operator;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}