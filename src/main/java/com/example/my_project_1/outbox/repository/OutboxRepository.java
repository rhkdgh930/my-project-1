package com.example.my_project_1.outbox.repository;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    Page<OutboxEvent> findAllByStatus(OutboxStatus status, Pageable pageable);

    @Query("""
            SELECT o.id
            FROM OutboxEvent o
            WHERE o.status IN ('PENDING', 'FAILED')
              AND o.nextRetryAt <= :now
              ORDER BY o.nextRetryAt ASC, o.id ASC
            """)
    List<Long> findProcessableIds(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OutboxEvent o
            SET o.status = 'PROCESSING',
                o.lastTriedAt = :now
            WHERE o.id = :id
              AND o.status IN ('PENDING', 'FAILED')
              AND o.nextRetryAt <= :now
            """)
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Query("""
            SELECT o.id
            FROM OutboxEvent o
            WHERE o.status = 'PROCESSING'
              AND o.lastTriedAt < :threshold
            ORDER BY o.lastTriedAt ASC, o.id ASC
            """)
    List<Long> findStuckProcessingIds(
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable
    );

    @Modifying
    @Query("""
            DELETE FROM OutboxEvent o
            WHERE o.status = 'SUCCESS'
              AND o.createdAt < :threshold
            """)
    int deleteSuccessBefore(@Param("threshold") LocalDateTime threshold);
}
