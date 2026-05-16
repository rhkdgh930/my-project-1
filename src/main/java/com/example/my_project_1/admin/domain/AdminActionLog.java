package com.example.my_project_1.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "admin_action_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminActionTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 500)
    private String description;

    @Lob
    @Column(nullable = false)
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static AdminActionLog create(
            Long adminId,
            AdminActionType actionType,
            AdminActionTargetType targetType,
            Long targetId,
            String description,
            String metadata,
            LocalDateTime createdAt
    ) {
        AdminActionLog log = new AdminActionLog();
        log.adminId = adminId;
        log.actionType = actionType;
        log.targetType = targetType;
        log.targetId = targetId;
        log.description = description;
        log.metadata = metadata == null ? "{}" : metadata;
        log.createdAt = createdAt;
        return log;
    }
}
