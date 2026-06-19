package com.bank.us.kafka;

import com.bank.us.enums.UserStatus;
import com.bank.us.model.UserProfile;
import com.bank.us.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.events.UserRegisteredEvent;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserRegistrationConsumer {

    private final UserProfileRepository
            repository;

    @KafkaListener(
            topics = KafkaTopics.USER_REGISTRATION_TOPIC,
            groupId ="users-group")
    public void consume(UserRegisteredEvent event) {

        repository.save( UserProfile.builder()
                        .userId(event.getUserId())
                        .username(event.getUsername())
                        .email(event.getEmail())
                        .status(UserStatus.ACTIVE.name())
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}