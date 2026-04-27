package com.example.my_project_1.outbox.repository;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

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

    @Modifying
    @Query("""
            UPDATE OutboxEvent o
            SET o.status = 'FAILED',
                o.nextRetryAt = :now
            WHERE o.status = 'PROCESSING'
              AND o.lastTriedAt < :threshold
            """)
    int recoverStuckEvents(@Param("threshold") LocalDateTime threshold,
                           @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            DELETE FROM OutboxEvent o
            WHERE o.status = 'SUCCESS'
              AND o.createdAt < :threshold
            """)
    int deleteSuccessBefore(@Param("threshold") LocalDateTime threshold);
}
