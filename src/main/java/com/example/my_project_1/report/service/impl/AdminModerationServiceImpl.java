package com.example.my_project_1.report.service.impl;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventKey;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.event.PostDeletedOutboxEvent;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.report.domain.Report;
import com.example.my_project_1.report.domain.ReportStatus;
import com.example.my_project_1.report.domain.ReportTargetType;
import com.example.my_project_1.report.repository.ReportRepository;
import com.example.my_project_1.report.service.AdminModerationService;
import com.example.my_project_1.report.service.response.ReportResponse;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.service.AdminUserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminModerationServiceImpl implements AdminModerationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final OutboxPublisher outboxPublisher;
    private final AdminUserCommandService adminUserCommandService;
    private final Clock clock;

    @Override
    public void deletePost(Long postId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        post.delete(LocalDateTime.now(clock));

        outboxPublisher.publish(
                OutboxEventType.POST_DELETED,
                DataSerializer.serialize(new PostDeletedOutboxEvent(post.getId(), post.getUserId())),
                OutboxEventKey.postDeleted(postId)
        );
    }

    @Override
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        postRepository.findActiveById(comment.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        comment.deleteByAdmin(LocalDateTime.now(clock));
    }

    @Override
    public ReportResponse deleteTargetByReport(Long reportId, Long reviewerId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getTargetType() == ReportTargetType.POST) {
            deletePost(report.getTargetId());
        } else if (report.getTargetType() == ReportTargetType.COMMENT) {
            deleteComment(report.getTargetId());
        } else {
            throw new CustomException(ErrorCode.UNSUPPORTED_REPORT_TARGET);
        }

        report.updateStatus(ReportStatus.ACTION_TAKEN, reviewerId, LocalDateTime.now(clock));
        return ReportResponse.from(report);
    }

    @Override
    public ReportResponse suspendUserByReport(
            Long reportId,
            Long reviewerId,
            SuspensionType type,
            SuspensionReason reason,
            Duration duration
    ) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getTargetType() != ReportTargetType.USER) {
            throw new CustomException(ErrorCode.UNSUPPORTED_REPORT_TARGET);
        }

        Long targetUserId = report.getTargetId();
        if (targetUserId.equals(reviewerId)) {
            throw new CustomException(ErrorCode.INVALID_USER_STATUS);
        }

        adminUserCommandService.suspendUser(targetUserId, type, reason, duration);

        report.updateStatus(ReportStatus.ACTION_TAKEN, reviewerId, LocalDateTime.now(clock));
        return ReportResponse.from(report);
    }
}
