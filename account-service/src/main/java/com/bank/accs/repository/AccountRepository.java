package com.bank.accs.repository;

import com.bank.accs.model.Account;
import com.bank.accs.model.enums.AccountStatus;
import com.bank.accs.model.enums.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM Account a
            WHERE a.accountNumber = :accountNumber
            """)
    Optional<Account> findByAccountNumberForUpdate(
            @Param("accountNumber") String accountNumber
    );

    List<Account> findByCustomerId(String customerId);

    boolean existsByAccountNumber(String accountNumber);

    Long countByCustomerId(String customerId);

    List<Account> findByUpdatedAtBefore(LocalDateTime date);

    List<Account> findByAvailableBalanceLessThan(BigDecimal amount);


    List<Account> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT a.accountType, COUNT(a)
            FROM Account a
            GROUP BY a.accountType
            """)
    List<Object[]> getAccountTypeStats();

    long count();

    long countByAccountStatus(AccountStatus status);

    long countByAccountType(AccountType accountType);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(a.availableBalance),0)
            FROM Account a
            """)
    BigDecimal getTotalBankBalance();

    @Query("""
            SELECT COALESCE(AVG(a.availableBalance),0)
            FROM Account a
            """)
    BigDecimal getAverageAccountBalance();


    List<Account> findByCustomerIdAndAccountStatus(String customerId, AccountStatus accountStatus);

    long countByCustomerIdAndAccountStatus(String customerId, AccountStatus accountStatus);

    boolean existsByCustomerIdAndAccountStatus(String customerId, AccountStatus accountStatus);

    @Query("""
            SELECT COALESCE(SUM(a.availableBalance),0)
            FROM Account a
            WHERE a.customerId=:customerId
            """)
    BigDecimal getCustomerTotalBalance(@Param("customerId") String customerId);

    @Query("""
            SELECT COALESCE(SUM(a.ledgerBalance),0)
            FROM Account a
            WHERE a.customerId=:customerId
            """)
    BigDecimal getCustomerLedgerBalance(@Param("customerId") String customerId);

    @Query("""
            SELECT COUNT(a)
            FROM Account a
            WHERE a.customerId=:customerId
            AND a.accountStatus<>'CLOSED'
            """)
    long countOpenAccounts(@Param("customerId") String customerId);

    @Query("""
            SELECT a.accountNumber
            FROM Account a
            WHERE a.customerId = :customerId
            AND a.deleted = false
            """)
    List<String> findAccountNumbersByCustomerId(@Param("customerId") String customerId);
}