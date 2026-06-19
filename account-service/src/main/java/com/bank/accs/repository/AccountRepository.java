package com.bank.accs.repository;

import com.bank.accs.model.Account;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(String customerId);

    boolean existsByAccountNumber(String accountNumber);

    Long countByCustomerId(String customerId);

    List<Account> findByUpdatedAtBefore(LocalDateTime date);

    List<Account> findByAvailableBalanceLessThan(BigDecimal amount);



    long countByAccountStatus(String status);

    List<Account> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a.accountType, COUNT(a) FROM Account a GROUP BY a.accountType")
    List<Object[]> getAccountTypeStats();
}