package com.bank.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class OperatorRegistry {

    public boolean evaluate(
            String operator,
            Object actualValue,
            String expectedValue,
            String dataType) {

        if (actualValue == null) {
            return false;
        }

        return switch (operator.toUpperCase()) {
            case "EQ" -> equalsValue(actualValue, expectedValue, dataType);
            case "NEQ" -> !equalsValue(actualValue, expectedValue, dataType);

            case "GT" -> compare(actualValue, expectedValue, dataType) > 0;
            case "GTE" -> compare(actualValue, expectedValue, dataType) >= 0;
            case "LT" -> compare(actualValue, expectedValue, dataType) < 0;
            case "LTE" -> compare(actualValue, expectedValue, dataType) <= 0;

            case "IN" -> in(actualValue, expectedValue, dataType);
            case "NOT_IN" -> !in(actualValue, expectedValue, dataType);

            case "CONTAINS" -> actualValue.toString()
                    .toLowerCase(Locale.ROOT)
                    .contains(expectedValue.toLowerCase(Locale.ROOT));

            default -> throw new IllegalArgumentException(
                    "Unsupported operator: " + operator
            );
        };
    }

    private boolean equalsValue(
            Object actualValue,
            String expectedValue,
            String dataType) {

        return normalize(actualValue, dataType)
                .equals(normalize(expectedValue, dataType));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(
            Object actualValue,
            String expectedValue,
            String dataType) {

        Comparable actual = (Comparable) normalize(actualValue, dataType);
        Comparable expected = (Comparable) normalize(expectedValue, dataType);

        return actual.compareTo(expected);
    }

    private boolean in(
            Object actualValue,
            String expectedValue,
            String dataType) {

        List<String> values = Arrays.stream(expectedValue.split(","))
                .map(String::trim)
                .toList();

        Object normalizedActual = normalize(actualValue, dataType);

        return values.stream()
                .map(value -> normalize(value, dataType))
                .anyMatch(normalizedActual::equals);
    }

    private Object normalize(Object value, String dataType) {
        String valueAsString = String.valueOf(value).trim();

        return switch (dataType.toUpperCase()) {
            case "NUMBER" -> new BigDecimal(valueAsString);

            case "BOOLEAN" -> Boolean.parseBoolean(valueAsString);

            case "DATE" -> LocalDate.parse(valueAsString);

            case "DATETIME" -> LocalDateTime.parse(valueAsString);

            case "STRING" -> valueAsString;

            default -> valueAsString;
        };
    }
}