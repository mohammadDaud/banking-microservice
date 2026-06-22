package com.bank.service.Impl;

import com.bank.dtos.RuleConditionRequest;
import com.bank.dtos.RuleConditionResponse;
import com.bank.dtos.RuleRequest;
import com.bank.dtos.RuleResponse;
import com.bank.enums.RuleType;
import com.bank.model.ConditionalOperator;
import com.bank.model.FieldMaster;
import com.bank.model.RuleCondition;
import com.bank.model.RuleMaster;
import com.bank.repository.ConditionalOperatorRepository;
import com.bank.repository.FieldMasterRepository;
import com.bank.repository.FieldOperatorMappingRepository;
import com.bank.repository.RuleMasterRepository;
import com.bank.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleMasterRepository ruleRepository;
    private final FieldMasterRepository fieldRepository;
    private final ConditionalOperatorRepository operatorRepository;
    private final FieldOperatorMappingRepository mappingRepository;

    @Override
    @Transactional
    public RuleResponse create(RuleRequest request) {
        String ruleCode = request.getRuleCode().trim().toUpperCase();

        if (ruleRepository.existsByRuleCode(ruleCode)) {
            throw new IllegalArgumentException(
                    "Rule code already exists: " + ruleCode
            );
        }

        validateUniqueSequenceOrders(request.getConditions());
        validateExpression(request.getExpression(), request.getConditions());

        LocalDateTime now = LocalDateTime.now();

        RuleMaster rule = RuleMaster.builder()
                .ruleCode(ruleCode)
                .ruleName(request.getRuleName().trim())
                .ruleType(request.getRuleType())
                .description(request.getDescription())
                .decision(request.getDecision())
                .priority(request.getPriority() == null ? 100 : request.getPriority())
                .expression(normalizeExpression(request.getExpression()))
                .isActive(request.getIsActive() == null || request.getIsActive())
                .createdBy(request.getRequestedBy())
                .updatedBy(request.getRequestedBy())
                .createdAt(now)
                .updatedAt(now)
                .build();

        addConditions(rule, request.getConditions(), now);

        return map(ruleRepository.save(rule));
    }

    @Override
    @Transactional
    public RuleResponse update(Long id, RuleRequest request) {
        RuleMaster rule = getEntity(id);

        validateUniqueSequenceOrders(request.getConditions());
        validateExpression(request.getExpression(), request.getConditions());

        LocalDateTime now = LocalDateTime.now();

        rule.setRuleName(request.getRuleName().trim());
        rule.setRuleType(request.getRuleType());
        rule.setDescription(request.getDescription());
        rule.setDecision(request.getDecision());
        rule.setPriority(request.getPriority() == null ? 100 : request.getPriority());
        rule.setExpression(normalizeExpression(request.getExpression()));
        rule.setIsActive(request.getIsActive() == null || request.getIsActive());
        rule.setUpdatedBy(request.getRequestedBy());
        rule.setUpdatedAt(now);

        /*
         * orphanRemoval = true in RuleMaster removes old conditions
         * when they are removed from this collection.
         */
        rule.getConditions().clear();

        addConditions(rule, request.getConditions(), now);

        return map(ruleRepository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public RuleResponse getById(Long id) {
        return map(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> getAll(
            RuleType ruleType,
            Boolean activeOnly) {

        List<RuleMaster> rules = ruleRepository.findAll();

        return rules.stream()
                .filter(rule ->
                        ruleType == null || rule.getRuleType() == ruleType
                )
                .filter(rule ->
                        !Boolean.TRUE.equals(activeOnly)
                                || Boolean.TRUE.equals(rule.getIsActive())
                )
                .sorted((first, second) ->
                        Integer.compare(first.getPriority(), second.getPriority())
                )
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RuleMaster rule = getEntity(id);
        ruleRepository.delete(rule);
    }

    private void addConditions(
            RuleMaster rule,
            List<RuleConditionRequest> requests,
            LocalDateTime now) {

        List<RuleCondition> conditions = new ArrayList<>();

        for (RuleConditionRequest request : requests) {

            FieldMaster field = fieldRepository
                    .findByFieldName(request.getFieldName().trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Field not found: " + request.getFieldName()
                    ));

            ConditionalOperator operator = operatorRepository
                    .findByShortName(
                            request.getOperatorShortName().trim().toUpperCase()
                    )
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Operator not found: "
                                    + request.getOperatorShortName()
                    ));

            if (!Boolean.TRUE.equals(field.getIsActive())) {
                throw new IllegalArgumentException(
                        "Field is inactive: " + field.getFieldName()
                );
            }

            if (!Boolean.TRUE.equals(operator.getIsActive())) {
                throw new IllegalArgumentException(
                        "Operator is inactive: " + operator.getShortName()
                );
            }

            if (!mappingRepository.existsByFieldIdAndOperatorId(
                    field.getId(),
                    operator.getId())) {

                throw new IllegalArgumentException(
                        "Operator " + operator.getShortName()
                                + " is not allowed for field "
                                + field.getFieldName()
                );
            }

            RuleCondition condition = RuleCondition.builder()
                    .rule(rule)
                    .field(field)
                    .operator(operator)
                    .conditionValue(request.getConditionValue().trim())
                    .sequenceOrder(request.getSequenceOrder())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            conditions.add(condition);
        }

        rule.getConditions().addAll(conditions);
    }

    private void validateUniqueSequenceOrders(
            List<RuleConditionRequest> conditions) {

        long distinctCount = conditions.stream()
                .map(RuleConditionRequest::getSequenceOrder)
                .distinct()
                .count();

        if (distinctCount != conditions.size()) {
            throw new IllegalArgumentException(
                    "Each rule condition must have a unique sequenceOrder"
            );
        }
    }

    private void validateExpression(
            String expression,
            List<RuleConditionRequest> conditions) {

        if (expression == null || expression.isBlank()) {
            return;
        }

        Set<Integer> validSequences = conditions.stream()
                .map(RuleConditionRequest::getSequenceOrder)
                .collect(Collectors.toSet());

        Matcher matcher = Pattern.compile("\\d+").matcher(expression);

        boolean hasConditionReference = false;

        while (matcher.find()) {
            hasConditionReference = true;

            Integer sequenceOrder = Integer.valueOf(matcher.group());

            if (!validSequences.contains(sequenceOrder)) {
                throw new IllegalArgumentException(
                        "Expression references invalid condition sequence: "
                                + sequenceOrder
                );
            }
        }

        if (!hasConditionReference) {
            throw new IllegalArgumentException(
                    "Expression must contain at least one condition sequence number"
            );
        }

        String remaining = expression
                .replaceAll("\\d+", "")
                .replaceAll("(?i)AND|OR", "")
                .replaceAll("[()\\s]", "");

        if (!remaining.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid expression. Use only condition numbers, "
                            + "AND, OR, and parentheses"
            );
        }

        validateParentheses(expression);
    }

    private void validateParentheses(String expression) {
        int count = 0;

        for (char character : expression.toCharArray()) {
            if (character == '(') {
                count++;
            } else if (character == ')') {
                count--;

                if (count < 0) {
                    throw new IllegalArgumentException(
                            "Invalid expression: parentheses do not match"
                    );
                }
            }
        }

        if (count != 0) {
            throw new IllegalArgumentException(
                    "Invalid expression: parentheses do not match"
            );
        }
    }

    private String normalizeExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        return expression
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    private RuleMaster getEntity(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rule not found: " + id
                ));
    }

    private RuleResponse map(RuleMaster rule) {
        List<RuleConditionResponse> conditions = rule.getConditions()
                .stream()
                .sorted((first, second) -> Integer.compare(
                        first.getSequenceOrder(),
                        second.getSequenceOrder()
                ))
                .map(condition -> RuleConditionResponse.builder()
                        .id(condition.getId())
                        .fieldName(condition.getField().getFieldName())
                        .displayName(condition.getField().getDisplayName())
                        .dataType(condition.getField().getDataType().name())
                        .operatorShortName(condition.getOperator().getShortName())
                        .operatorSymbol(condition.getOperator().getSymbol())
                        .conditionValue(condition.getConditionValue())
                        .sequenceOrder(condition.getSequenceOrder())
                        .build())
                .toList();

        return RuleResponse.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .description(rule.getDescription())
                .decision(rule.getDecision())
                .priority(rule.getPriority())
                .expression(rule.getExpression())
                .isActive(rule.getIsActive())
                .conditions(conditions)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}