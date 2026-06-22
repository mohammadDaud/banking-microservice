package com.bank.service.Impl;

import com.bank.dtos.EvaluationStep;
import com.bank.dtos.RuleEvaluationRequest;
import com.bank.dtos.RuleEvaluationResponse;
import com.bank.enums.RuleDecision;
import com.bank.model.RuleCondition;
import com.bank.model.RuleMaster;
import com.bank.repository.RuleMasterRepository;
import com.bank.service.RuleEvaluationService;
import com.bank.util.ExpressionParser;
import com.bank.util.OperatorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuleEvaluationServiceImpl implements RuleEvaluationService {

    private final RuleMasterRepository ruleRepository;
    private final OperatorRegistry operatorRegistry;
    private final ExpressionParser expressionParser;

    @Override
    @Transactional(readOnly = true)
    public RuleEvaluationResponse evaluate(RuleEvaluationRequest request) {

        List<RuleMaster> rules =
                ruleRepository.findByRuleTypeAndIsActiveTrueOrderByPriorityAsc(
                        request.getRuleType()
                );

        List<EvaluationStep> allSteps = new ArrayList<>();

        for (RuleMaster rule : rules) {

            /*
             * key = RuleCondition.sequenceOrder
             * value = result of that condition
             *
             * Example:
             * 1 -> true
             * 2 -> false
             * 3 -> true
             */
            Map<Integer, Boolean> conditionResults = new LinkedHashMap<>();

            for (RuleCondition condition : rule.getConditions()) {

                String fieldName = condition.getField().getFieldName();

                Object actualValue = getValueByPath(
                        request.getPayload(),
                        fieldName
                );

                boolean conditionMatched;
                String message;

                try {
                    conditionMatched = operatorRegistry.evaluate(
                            condition.getOperator().getShortName(),
                            actualValue,
                            condition.getConditionValue(),
                            condition.getField().getDataType().name()
                    );

                    message = conditionMatched
                            ? "Condition matched"
                            : "Condition did not match";

                } catch (Exception exception) {
                    conditionMatched = false;
                    message = "Condition evaluation failed: "
                            + exception.getMessage();
                }

                conditionResults.put(
                        condition.getSequenceOrder(),
                        conditionMatched
                );

                allSteps.add(
                        EvaluationStep.builder()
                                .ruleId(rule.getId())
                                .ruleCode(rule.getRuleCode())
                                .ruleName(rule.getRuleName())
                                .conditionId(condition.getId())
                                .fieldName(fieldName)
                                .operator(condition.getOperator().getShortName())
                                .expectedValue(condition.getConditionValue())
                                .actualValue(actualValue)
                                .matched(conditionMatched)
                                .message(message)
                                .build()
                );
            }

            boolean ruleMatched;

            try {
                ruleMatched = expressionParser.evaluate(
                        rule.getExpression(),
                        conditionResults
                );
            } catch (Exception exception) {
                /*
                 * A bad saved expression must not approve a banking action.
                 * It is treated as not matched and steps show the reason.
                 */
                ruleMatched = false;

                allSteps.add(
                        EvaluationStep.builder()
                                .ruleId(rule.getId())
                                .ruleCode(rule.getRuleCode())
                                .ruleName(rule.getRuleName())
                                .matched(false)
                                .message("Rule expression evaluation failed: "
                                        + exception.getMessage())
                                .build()
                );
            }

            if (ruleMatched) {
                return RuleEvaluationResponse.builder()
                        .decision(rule.getDecision())
                        .matchedRuleCode(rule.getRuleCode())
                        .reason("Matched rule: " + rule.getRuleName())
                        .evaluationSteps(allSteps)
                        .build();
            }
        }

        return RuleEvaluationResponse.builder()
                .decision(RuleDecision.AUTO_APPROVE)
                .matchedRuleCode(null)
                .reason("No active rule matched. Default decision is AUTO_APPROVE")
                .evaluationSteps(allSteps)
                .build();
    }

    /**
     * Supports payload paths:
     *
     * amount
     * customer.type
     * transaction.bankType
     */
    @SuppressWarnings("unchecked")
    private Object getValueByPath(
            Map<String, Object> payload,
            String fieldPath) {

        if (payload == null || fieldPath == null || fieldPath.isBlank()) {
            return null;
        }

        Object currentValue = payload;

        for (String part : fieldPath.split("\\.")) {

            if (!(currentValue instanceof Map<?, ?> currentMap)) {
                return null;
            }

            currentValue = ((Map<String, Object>) currentMap).get(part);

            if (currentValue == null) {
                return null;
            }
        }

        return currentValue;
    }
}