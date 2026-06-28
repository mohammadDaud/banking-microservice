package com.bank.accs.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    private IpUtil() {}

    public static String getClientIp(
            HttpServletRequest request) {

        String forwarded =
                request.getHeader(
                        "X-Forwarded-For");

        if (forwarded != null
                && !forwarded.isBlank()) {

            return forwarded.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}