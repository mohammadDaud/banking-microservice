package com.bank.as.repository;

import com.bank.as.model.entites.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, String> {
    List<PasswordHistory> findTop3ByUserIdOrderByCreatedAtDesc(String userId);
}
