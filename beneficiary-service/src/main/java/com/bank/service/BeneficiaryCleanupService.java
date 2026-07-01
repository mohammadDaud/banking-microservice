package com.bank.service;

import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.CustomerDeletedEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.util.EventMetadataUtil;
import com.bank.enums.BeneficiaryStatus;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.Beneficiary;
import com.bank.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryCleanupService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final BeneficiaryRepository beneficiaryRepository;

    private final KafkaEventPublisher eventPublisher;

    public void deactivateBeneficiaries(CustomerDeletedEvent event) {
        if (event == null || event.getAccountNumbers() == null || event.getAccountNumbers().isEmpty()) {
            log.info("No account numbers found for customer {}", event.getUserId());
            return;
        }
        log.info("Starting beneficiary cleanup for customer {}", event.getUserId());
        List<Beneficiary> beneficiaries =
                beneficiaryRepository.findByBeneficiaryAccountNumberIn(event.getAccountNumbers());
        if (beneficiaries.isEmpty()) {
            log.info("No beneficiary records found.");
            return;
        }
        updateBeneficiaries(beneficiaries);
        beneficiaryRepository.saveAll(beneficiaries);
        publishAuditEvent(event, beneficiaries.size());

    }

    private void updateBeneficiaries(List<Beneficiary> beneficiaries) {
        LocalDateTime now = LocalDateTime.now();
        beneficiaries.forEach(beneficiary -> {
            if (beneficiary.getStatus() == BeneficiaryStatus.INACTIVE) {
                return;
            }
            beneficiary.setStatus(BeneficiaryStatus.INACTIVE);
            beneficiary.setUpdatedAt(now);
            beneficiary.setUpdatedBy(SYSTEM_USER);
        });
    }

    private void publishAuditEvent(CustomerDeletedEvent event,int affectedCount) {
        eventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(event.getCorrelationId())
                        .requestId(event.getRequestId())
                        .serviceName("beneficiary-service")
                        .source(EventSource.BENEFICIARY_SERVICE)
                        .status(EventStatus.SUCCESS)
                        .userId(SYSTEM_USER)
                        .username(SYSTEM_USER)
                        .module("BENEFICIARY")
                        .action("DEACTIVATE_BENEFICIARIES")
                        .entityId(event.getUserId())
                        .entityType("CUSTOMER")
                        .description(affectedCount +" beneficiary record(s) marked INACTIVE because customer was deleted.")
                        .createdAt(LocalDateTime.now())
                        .build()

        );
    }
}