package com.bank.repository;

import com.bank.enums.TransactionStatus;
import com.bank.model.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository  extends JpaRepository<Transaction,String> {

    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(String customerId);

    List<Transaction> findBySourceAccountOrderByTransactionDateDesc(String accountNumber);

    Long countByCustomerId(String customerId);

    List<Transaction> findTop5ByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    @Query("""
    SELECT COUNT(t)
    FROM Transaction t
    WHERE t.transactionDate >= :startDate
    AND t.transactionDate < :endDate
""")
    long countTodayTransactions(
            LocalDateTime startDate,
            LocalDateTime endDate);

    List<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    @Query(value = """
       SELECT
       TO_CHAR(transactionDate,'Mon'),
       SUM(amount)
       FROM Transaction
       GROUP BY TO_CHAR(transactionDate,'Mon')
       ORDER BY MIN(transactionDate)
       """, nativeQuery = true)
    List<Object[]> getMonthlyStats();

    List<Transaction> findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
            String accountNumber, LocalDateTime from, LocalDateTime to);

    @Query("""
    SELECT COALESCE(SUM(t.amount),0)
    FROM Transaction t
    WHERE t.sourceAccount = :accountNumber
    AND t.transactionDate >= :start
    AND t.transactionDate < :end
    AND t.transactionStatus='SUCCESS'
    """)
    BigDecimal getTodayTransferAmount(String accountNumber,LocalDateTime start,LocalDateTime end);

        @Query("""
    SELECT COALESCE(SUM(t.amount),0)
    FROM Transaction t
    WHERE t.sourceAccount = :accountNumber
    AND t.transactionDate >= :start
    AND t.transactionDate < :end
    AND t.transactionStatus='SUCCESS'
    """)
    BigDecimal getMonthlyTransferAmount(String accountNumber,LocalDateTime start,LocalDateTime end);
    long countByTransactionDateBetween(LocalDateTime start,LocalDateTime end);

        @Query("""
    SELECT COALESCE(SUM(t.amount),0)
    FROM Transaction t
    WHERE t.transactionDate
    BETWEEN :start AND :end
    """)
    BigDecimal getTotalAmount(LocalDateTime start,LocalDateTime end);
    long countByTransactionStatus(TransactionStatus status);
    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDateTime start,LocalDateTime end);

    List<Transaction> findByAmountGreaterThan(BigDecimal amount);

    List<Transaction> findByTransactionStatusOrderByCreatedAtDesc(TransactionStatus transactionStatus);

    Optional<Transaction> findByIdAndTransactionStatus(String id, TransactionStatus transactionStatus);
}
