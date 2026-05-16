package com.example.my_project_1.admin.repository;

import com.example.my_project_1.admin.domain.AdminActionLog;
import com.example.my_project_1.admin.service.request.AdminActionLogSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminActionLogRepositoryCustom {

    Page<AdminActionLog> searchLogs(AdminActionLogSearchCondition condition, Pageable pageable);
}
