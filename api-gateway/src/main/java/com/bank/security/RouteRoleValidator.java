package com.bank.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public class RouteRoleValidator {

    public boolean hasAccess(
            String path,
            HttpMethod method,
            List<String> roles) {

        if (roles == null || roles.isEmpty()) {
            return false;
        }

        /*
         * Internal Account Service balance APIs.
         *
         * These are never allowed through API Gateway.
         * Transaction Service calls Account Service directly using
         * a short-lived ROLE_INTERNAL_SERVICE JWT.
         */
        if (isInternalAccountBalanceEndpoint(path, method)) {
            return false;
        }

        /*
         * Transaction maker-checker endpoints:
         * ADMIN and CHECKER can view/approve/reject pending transfers.
         */
        if (path.startsWith("/api/admin/transactions/pending")
                || path.matches("^/api/admin/transactions/[^/]+/(approve|reject)$")) {

            return hasAnyRole(roles, "ROLE_ADMIN", "ROLE_CHECKER");
        }

        /*
         * Other admin APIs remain ADMIN-only.
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

    private boolean isInternalAccountBalanceEndpoint(
            String path,
            HttpMethod method) {

        if (method != HttpMethod.PUT) {
            return false;
        }

        return path.matches("^/api/accounts/[^/]+/(credit|debit)$");
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