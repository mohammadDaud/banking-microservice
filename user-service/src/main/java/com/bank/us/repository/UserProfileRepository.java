package com.bank.us.repository;

import com.bank.us.model.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUsername(String username);
    long count();
    long countByStatus(String status);
    long countByCreatedAtBetween(LocalDateTime start,LocalDateTime end);

    List<UserProfile> findAllByOrderByCreatedAtDesc(Pageable pageable);
}