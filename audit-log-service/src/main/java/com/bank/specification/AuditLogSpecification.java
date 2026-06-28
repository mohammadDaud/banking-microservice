package com.bank.specification;

import com.bank.dtos.AuditLogSearchRequest;
import com.bank.model.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> search(AuditLogSearchRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (request.getUserId() != null && !request.getUserId().isBlank()) {
                predicates.add(cb.equal(root.get("userId"), request.getUserId()));
            }

            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("username")),
                                "%" + request.getUsername().toLowerCase() + "%"
                        )
                );
            }

            if (request.getModule() != null) {
                predicates.add(
                        cb.equal(root.get("module"), request.getModule())
                );
            }

            if (request.getAction() != null) {
                predicates.add(
                        cb.equal(root.get("action"), request.getAction())
                );
            }

            if (request.getEntityId() != null && !request.getEntityId().isBlank()) {
                predicates.add(
                        cb.equal(root.get("entityId"), request.getEntityId())
                );
            }

            if (request.getEntityType() != null && !request.getEntityType().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("entityType")),
                                "%" + request.getEntityType().toLowerCase() + "%"
                        )
                );
            }

            if (request.getFromDate() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                request.getFromDate().atStartOfDay()
                        )
                );
            }

            if (request.getToDate() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("createdAt"),
                                request.getToDate().atTime(23, 59, 59)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}