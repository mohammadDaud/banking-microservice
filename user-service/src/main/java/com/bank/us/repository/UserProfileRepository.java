package com.bank.us.repository;

import com.bank.us.model.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    long countByStatus(String status);

    List<UserProfile> findAllByOrderByCreatedAtDesc(Pageable pageable);
}