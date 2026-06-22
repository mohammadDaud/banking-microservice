package com.bank.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Stack;

@Component
public class ExpressionParser {

    /**
     * Examples:
     * (1 AND 2) OR 3
     * 1 AND 2 AND 3
     *
     * Condition numbers refer to RuleCondition.sequenceOrder.
     */
    public boolean evaluate(String expression,Map<Integer, Boolean> conditionResults) {

        // If no expression is saved, default behavior is:
        // condition 1 AND condition 2 AND condition 3
        if (expression == null || expression.isBlank()) {
            return conditionResults.values()
                    .stream()
                    .allMatch(Boolean::booleanValue);
        }

        String normalized = expression
                .replaceAll("(?i)AND", " AND ")
                .replaceAll("(?i)OR", " OR ")
                .replace("(", " ( ")
                .replace(")", " ) ")
                .trim()
                .replaceAll("\\s+", " ");

        String[] tokens = normalized.split(" ");

        Stack<Boolean> values = new Stack<>();
        Stack<String> operators = new Stack<>();

        for (String token : tokens) {

            if (token == null || token.isBlank()) {
                continue;
            }

            if (token.matches("\\d+")) {
                Integer sequenceOrder = Integer.valueOf(token);

                if (!conditionResults.containsKey(sequenceOrder)) {
                    throw new IllegalArgumentException(
                            "Expression references missing condition sequence: "
                                    + sequenceOrder
                    );
                }

                values.push(conditionResults.get(sequenceOrder));
                continue;
            }

            if ("(".equals(token)) {
                operators.push(token);
                continue;
            }

            if (")".equals(token)) {
                while (!operators.isEmpty()
                        && !"(".equals(operators.peek())) {

                    applyOperator(values, operators.pop());
                }

                if (operators.isEmpty() || !"(".equals(operators.peek())) {
                    throw new IllegalArgumentException(
                            "Invalid expression: parentheses do not match"
                    );
                }

                operators.pop();
                continue;
            }

            if ("AND".equalsIgnoreCase(token)
                    || "OR".equalsIgnoreCase(token)) {

                String currentOperator = token.toUpperCase();

                while (!operators.isEmpty()
                        && !"(".equals(operators.peek())
                        && precedence(operators.peek())
                        >= precedence(currentOperator)) {

                    applyOperator(values, operators.pop());
                }

                operators.push(currentOperator);
                continue;
            }

            throw new IllegalArgumentException(
                    "Invalid expression token: " + token
            );
        }

        while (!operators.isEmpty()) {
            if ("(".equals(operators.peek())) {
                throw new IllegalArgumentException(
                        "Invalid expression: parentheses do not match"
                );
            }

            applyOperator(values, operators.pop());
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException(
                    "Invalid rule expression"
            );
        }

        return values.pop();
    }

    private int precedence(String operator) {
        return switch (operator) {
            case "AND" -> 2;
            case "OR" -> 1;
            default -> 0;
        };
    }

    private void applyOperator(
            Stack<Boolean> values,
            String operator) {

        if (values.size() < 2) {
            throw new IllegalArgumentException(
                    "Invalid expression near operator: " + operator
            );
        }

        boolean right = values.pop();
        boolean left = values.pop();

        boolean result = switch (operator) {
            case "AND" -> left && right;
            case "OR" -> left || right;
            default -> throw new IllegalArgumentException(
                    "Unsupported logical operator: " + operator
            );
        };

        values.push(result);
    }
}