package com.bank.us.repository;

import com.bank.us.enums.UserStatus;
import com.bank.us.model.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, String>, JpaSpecificationExecutor<UserProfile> {

    Optional<UserProfile> findByUsername(String username);

    Optional<UserProfile> findByUserIdAndDeletedFalse(String userId);

    List<UserProfile> findAllByDeletedFalse();

    List<UserProfile> findAllByDeletedTrue();

    List<UserProfile> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(UserStatus status);

    long countByCreatedAtBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end);

}