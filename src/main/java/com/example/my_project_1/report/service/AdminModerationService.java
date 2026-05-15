package com.example.my_project_1.report.service;

import com.example.my_project_1.report.service.response.ReportResponse;

public interface AdminModerationService {

    void deletePost(Long postId);

    void deleteComment(Long commentId);

    ReportResponse deleteTargetByReport(Long reportId, Long reviewerId);
}
