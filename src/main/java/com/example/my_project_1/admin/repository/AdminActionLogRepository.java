package com.example.my_project_1.admin.repository;

import com.example.my_project_1.admin.domain.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long>, AdminActionLogRepositoryCustom {
}
