package com.bank.us.specification;

import com.bank.us.model.UserProfile;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class UserProfileSpecification {
    private UserProfileSpecification() {
    }

    public static Specification<UserProfile> search(String keyword) {

        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String value = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")),value),
                    cb.like(cb.lower(root.get("lastName")),value),
                    cb.like(cb.lower(root.get("username")),value),
                    cb.like(cb.lower(root.get("email")),value),
                    cb.like(cb.lower(root.get("mobileNumber")),value)
            );
        };
    }

    public static Specification<UserProfile> status(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<UserProfile> gender(String gender) {
        return (root, query, cb) -> {
            if (gender == null || gender.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("gender"), gender);
        };
    }

    public static Specification<UserProfile> notDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));

    }

    public static Specification<UserProfile> deleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("deleted"), deleted);
        };
    }

    public static Specification<UserProfile> createdBetween(LocalDateTime from,LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<UserProfile> nationality(String nationality) {
        return (root, query, cb) -> {
            if (nationality == null || nationality.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("nationality"), nationality);
        };
    }

    public static Specification<UserProfile> maritalStatus(String maritalStatus) {
        return (root, query, cb) -> {
            if (maritalStatus == null || maritalStatus.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("maritalStatus"), maritalStatus);
        };
    }
}
