package com.bank.accs.repository;

import com.bank.accs.model.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionLimitRepository extends JpaRepository<TransactionLimit,String> {
    Optional<TransactionLimit> findByAccountNumber(String accountNumber);
}