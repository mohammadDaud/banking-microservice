package com.bank.repository;

import com.bank.enums.BeneficiaryStatus;
import com.bank.model.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary,String> {
    List<Beneficiary> findByCustomerId(String customerId);
    boolean existsByCustomerIdAndAccountNumber(String customerId,String accountNumber);
    Long countByCustomerId(String customerId);
    List<Beneficiary> findByStatus(BeneficiaryStatus status);


    long count();
    long countByStatus(BeneficiaryStatus status);
    long countByCreatedAtBetween(LocalDateTime start,LocalDateTime end);

}