package com.bank.us.kafka;

import com.bank.common.events.UserRegisteredEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.us.enums.UserStatus;
import com.bank.us.model.UserProfile;
import com.bank.us.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationConsumer {

    private final UserProfileRepository
            repository;

    @KafkaListener(
            topics = KafkaTopics.USER_REGISTRATION_TOPIC,
            groupId = "users-group"
    )
    public void consume(UserRegisteredEvent event) {
        log.info("Received user registration event for userId={}", event.getUserId());
        if (event == null) {
            log.warn("Received null UserRegisteredEvent.");
            return;
        }

        if (repository.findById(event.getUserId()).isPresent()) {
            log.warn("User already exists. Skipping duplicate event. userId={}", event.getUserId());
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .email(event.getEmail())
                .status(UserStatus.ACTIVE)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            repository.save(profile);
            log.info("User profile created successfully. userId={}", profile.getUserId());
        } catch (Exception ex) {
            log.error("Unable to create profile for userId={}", event.getUserId(), ex);
        }
    }
}