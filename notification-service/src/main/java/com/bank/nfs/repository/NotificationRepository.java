package com.bank.nfs.repository;

import com.bank.nfs.model.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    Long countByUserIdAndReadFlag(String userId,Boolean readFlag);
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(String userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc( String userId, Pageable pageable);

            @Modifying
            @Transactional
            @Query("""
        UPDATE Notification n
        SET n.readFlag = true
        WHERE n.userId = :userId
        AND n.readFlag = false
        """)
    void markAllRead(String userId);

    void deleteByCreatedAtBefore(LocalDateTime date);

    boolean existsByEventId(String eventId);

    /*===============DASHBOARD============*/
    long count();
    long countByReadFlag(Boolean readFlag);
    long countByCreatedAtBetween(LocalDateTime start,LocalDateTime end);
}