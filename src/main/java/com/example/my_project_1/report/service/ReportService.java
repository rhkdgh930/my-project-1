package com.example.my_project_1.report.service;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.report.service.request.ReportCreateRequest;
import com.example.my_project_1.report.service.request.ReportStatusUpdateRequest;
import com.example.my_project_1.report.service.response.ReportResponse;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportResponse create(Long reporterId, ReportCreateRequest request);

    PageResponse<ReportResponse> findReports(Pageable pageable);

    ReportResponse findReport(Long reportId);

    ReportResponse updateStatus(Long reportId, Long reviewerId, ReportStatusUpdateRequest request);
}
