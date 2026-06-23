package com.bank.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RouteRoleValidator {

    private static final Map<String, String> ROLE_MAPPING =
            Map.of(
                    "/api/admin/", "ROLE_ADMIN",
                    "/api/customer/", "ROLE_CUSTOMER",
                    "/api/manager/", "ROLE_MANAGER"
            );

    public boolean hasAccess(String path, List<String> roles) {

        if (roles == null || roles.isEmpty()) {
            return false;
        }

        /*
         * Transaction maker-checker endpoints:
         * both ADMIN and CHECKER can view/approve/reject pending transfers.
         */
        if (path.startsWith("/api/admin/transactions/pending")
                || path.matches("^/api/admin/transactions/[^/]+/(approve|reject)$")) {

            return hasAnyRole(roles, "ROLE_ADMIN", "ROLE_CHECKER");
        }

        /*
         * Other /api/admin/** APIs remain ADMIN-only.
         */
        if (path.startsWith("/api/admin/")) {
            return hasRole(roles, "ROLE_ADMIN");
        }

        if (path.startsWith("/api/customer/")) {
            return hasRole(roles, "ROLE_CUSTOMER");
        }

        if (path.startsWith("/api/manager/")) {
            return hasRole(roles, "ROLE_MANAGER");
        }

        return true;
    }

    private boolean hasRole(List<String> roles, String requiredRole) {
        return roles.stream()
                .anyMatch(role -> requiredRole.equalsIgnoreCase(role));
    }

    private boolean hasAnyRole(
            List<String> roles,
            String... requiredRoles) {

        for (String requiredRole : requiredRoles) {
            if (hasRole(roles, requiredRole)) {
                return true;
            }
        }

        return false;
    }
}