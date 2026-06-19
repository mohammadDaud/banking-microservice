package com.bank.repository;

import com.bank.model.Nominee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NomineeRepository extends JpaRepository<Nominee,String> {
    List<Nominee> findByCustomerId(String customerId);
    List<Nominee> findByAccountNumber(String accountNumber);

}