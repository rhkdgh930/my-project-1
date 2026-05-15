package com.example.my_project_1.report.service.impl;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.report.domain.Report;
import com.example.my_project_1.report.domain.ReportTargetType;
import com.example.my_project_1.report.repository.ReportRepository;
import com.example.my_project_1.report.service.ReportService;
import com.example.my_project_1.report.service.request.ReportCreateRequest;
import com.example.my_project_1.report.service.request.ReportStatusUpdateRequest;
import com.example.my_project_1.report.service.response.ReportResponse;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ReportResponse create(Long reporterId, ReportCreateRequest request) {
        validateTarget(reporterId, request);
        validateNotDuplicated(reporterId, request.targetType(), request.targetId());

        try {
            Report report = Report.create(
                    request.targetType(),
                    request.targetId(),
                    reporterId,
                    request.reason(),
                    request.content()
            );
            return ReportResponse.from(reportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATED_REPORT);
        }
    }

    @Override
    public PageResponse<ReportResponse> findReports(Pageable pageable) {
        Page<ReportResponse> page = reportRepository.findAll(pageable)
                .map(ReportResponse::from);
        return PageResponse.of(page);
    }

    @Override
    public ReportResponse findReport(Long reportId) {
        return ReportResponse.from(findById(reportId));
    }

    @Override
    @Transactional
    public ReportResponse updateStatus(Long reportId, Long reviewerId, ReportStatusUpdateRequest request) {
        Report report = findById(reportId);
        report.updateStatus(request.status(), reviewerId, LocalDateTime.now(clock));
        return ReportResponse.from(report);
    }

    private void validateTarget(Long reporterId, ReportCreateRequest request) {
        ReportTargetType targetType = request.targetType();
        if (targetType == ReportTargetType.POST) {
            validatePostTarget(request.targetId());
            return;
        }
        if (targetType == ReportTargetType.COMMENT) {
            validateCommentTarget(request.targetId());
            return;
        }
        validateUserTarget(reporterId, request.targetId());
    }

    private void validatePostTarget(Long postId) {
        postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateCommentTarget(Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        postRepository.findActiveById(comment.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateUserTarget(Long reporterId, Long targetId) {
        if (reporterId.equals(targetId)) {
            throw new CustomException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.isDeleted() || user.isWithdrawnCompletely()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateNotDuplicated(Long reporterId, ReportTargetType targetType, Long targetId) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
            throw new CustomException(ErrorCode.DUPLICATED_REPORT);
        }
    }

    private Report findById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
    }
}
