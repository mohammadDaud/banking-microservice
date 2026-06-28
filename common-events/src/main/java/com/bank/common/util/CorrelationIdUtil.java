package com.bank.common.util;

import com.bank.common.constants.CorrelationConstants;
import org.slf4j.MDC;

public final class CorrelationIdUtil {

    private CorrelationIdUtil() {
    }

    public static String getCorrelationId() {
        return MDC.get(CorrelationConstants.MDC_KEY);
    }

    public static void setCorrelationId(String correlationId) {
        MDC.put(CorrelationConstants.MDC_KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(CorrelationConstants.MDC_KEY);
    }
}