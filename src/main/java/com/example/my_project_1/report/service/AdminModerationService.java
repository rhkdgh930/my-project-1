package com.example.my_project_1.report.service;

import com.example.my_project_1.report.service.response.ReportResponse;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;

import java.time.Duration;

public interface AdminModerationService {

    void deletePost(Long postId);

    void deleteComment(Long commentId);

    ReportResponse deleteTargetByReport(Long reportId, Long reviewerId);

    ReportResponse suspendUserByReport(
            Long reportId,
            Long reviewerId,
            SuspensionType type,
            SuspensionReason reason,
            Duration duration
    );
}
