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

    public boolean hasAccess(String path,List<String> roles) {
        if (path.startsWith("/api/admin/")) {
            return roles.contains("ROLE_ADMIN");
        }

        if (path.startsWith("/api/customer/")) {
            return roles.contains("ROLE_CUSTOMER");
        }

        if (path.startsWith("/api/manager/")) {
            return roles.contains("ROLE_MANAGER");
        }
        return true;
    }
}