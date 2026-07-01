package com.bank.kafka.consumer;

import com.bank.common.events.CustomerDeletedEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.service.BeneficiaryCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerDeletedConsumer {

    private final BeneficiaryCleanupService cleanupService;

    @KafkaListener(
            topics = KafkaTopics.CUSTOMER_DELETED_TOPIC,
            groupId = "beneficiary-service"
    )
    public void consume(CustomerDeletedEvent event) {
        log.info("Received CUSTOMER_DELETED event for customer {}",event.getUserId());
        try {
            cleanupService.deactivateBeneficiaries(event);
        } catch (Exception ex) {
            log.error("Failed to cleanup beneficiaries for customer {}",event.getUserId(),ex);
        }
    }
}