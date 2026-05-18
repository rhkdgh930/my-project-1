package com.example.my_project_1.report.service.impl;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.AdminActionLogService;
import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.monitoring.MonitoringService;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.report.domain.Report;
import com.example.my_project_1.report.domain.ReportStatus;
import com.example.my_project_1.report.domain.ReportTargetType;
import com.example.my_project_1.report.repository.ReportRepository;
import com.example.my_project_1.report.service.response.ReportResponse;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.service.AdminUserCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminModerationServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-15T01:02:03Z"),
            ZoneId.of("Asia/Seoul")
    );

    private PostRepository postRepository;
    private CommentRepository commentRepository;
    private ReportRepository reportRepository;
    private OutboxPublisher outboxPublisher;
    private AdminUserCommandService adminUserCommandService;
    private AdminActionLogService adminActionLogService;
    private MonitoringService monitoringService;
    private AdminModerationServiceImpl service;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        commentRepository = mock(CommentRepository.class);
        reportRepository = mock(ReportRepository.class);
        outboxPublisher = mock(OutboxPublisher.class);
        adminUserCommandService = mock(AdminUserCommandService.class);
        adminActionLogService = mock(AdminActionLogService.class);
        monitoringService = mock(MonitoringService.class);
        service = new AdminModerationServiceImpl(
                postRepository,
                commentRepository,
                reportRepository,
                outboxPublisher,
                adminUserCommandService,
                adminActionLogService,
                monitoringService,
                CLOCK
        );
    }

    @Test
    @DisplayName("관리자 게시글 삭제는 active post를 soft delete하고 POST_DELETED outbox를 발행한다.")
    void deletePost_deletesPostAndPublishesOutbox() {
        Post post = mock(Post.class);
        when(post.getId()).thenReturn(10L);
        when(post.getUserId()).thenReturn(20L);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(post));

        service.deletePost(10L);

        verify(post).delete(LocalDateTime.now(CLOCK));
        verify(outboxPublisher).publish(eq(OutboxEventType.POST_DELETED), anyString(), eq("POST_DELETED:10"));
    }

    @Test
    @DisplayName("없는 게시글을 관리자 삭제하면 POST_NOT_FOUND로 실패한다.")
    void deletePost_rejectsMissingPost() {
        when(postRepository.findActiveById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePost(10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 댓글 삭제는 active comment와 active post를 검증하고 tombstone 처리한다.")
    void deleteComment_deletesComment() {
        Comment comment = Comment.createRoot(10L, 20L, "댓글 내용");
        ReflectionTestUtils.setField(comment, "id", 100L);
        when(commentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(mock(Post.class)));

        service.deleteComment(100L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo(Comment.DELETED_CONTENT);
        assertThat(comment.getDeletedAt()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    @DisplayName("관리자 직접 게시글 삭제 성공 시 감사 로그를 저장한다.")
    void deletePostByAdmin_writesAuditLog() {
        Post post = mock(Post.class);
        when(post.getId()).thenReturn(10L);
        when(post.getUserId()).thenReturn(20L);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(post));

        service.deletePost(10L, 99L);

        verify(adminActionLogService).log(
                99L,
                AdminActionType.MODERATION_DELETE_POST,
                AdminActionTargetType.POST,
                10L,
                "관리자가 게시글을 삭제했습니다.",
                Map.of()
        );
        verify(monitoringService).recordAdminModerationAction(AdminActionType.MODERATION_DELETE_POST, ReportTargetType.POST.name());
    }

    @Test
    @DisplayName("관리자 직접 댓글 삭제 성공 시 감사 로그를 저장한다.")
    void deleteCommentByAdmin_writesAuditLog() {
        Comment comment = Comment.createRoot(10L, 20L, "댓글 내용");
        ReflectionTestUtils.setField(comment, "id", 100L);
        when(commentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(mock(Post.class)));

        service.deleteComment(100L, 99L);

        verify(adminActionLogService).log(
                99L,
                AdminActionType.MODERATION_DELETE_COMMENT,
                AdminActionTargetType.COMMENT,
                100L,
                "관리자가 댓글을 삭제했습니다.",
                Map.of()
        );
        verify(monitoringService).recordAdminModerationAction(AdminActionType.MODERATION_DELETE_COMMENT, ReportTargetType.COMMENT.name());
    }

    @Test
    @DisplayName("삭제됐거나 없는 댓글을 관리자 삭제하면 COMMENT_NOT_FOUND로 실패한다.")
    void deleteComment_rejectsMissingComment() {
        when(commentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteComment(100L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 신고 대상 삭제 조치는 게시글을 삭제하고 신고 상태를 ACTION_TAKEN으로 변경한다.")
    void deleteTargetByReport_deletesPostAndMarksActionTaken() {
        Report report = report(1L, ReportTargetType.POST, 10L);
        Post post = mock(Post.class);
        when(post.getId()).thenReturn(10L);
        when(post.getUserId()).thenReturn(20L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(post));

        ReportResponse response = service.deleteTargetByReport(1L, 99L);

        verify(post).delete(LocalDateTime.now(CLOCK));
        verify(outboxPublisher).publish(eq(OutboxEventType.POST_DELETED), anyString(), eq("POST_DELETED:10"));
        assertThat(response.status()).isEqualTo(ReportStatus.ACTION_TAKEN);
        assertThat(response.reviewerId()).isEqualTo(99L);
        assertThat(response.reviewedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        verify(adminActionLogService).log(
                eq(99L),
                eq(AdminActionType.REPORT_DELETE_TARGET),
                eq(AdminActionTargetType.REPORT),
                eq(1L),
                eq("관리자가 신고 대상 삭제 조치를 완료했습니다."),
                org.mockito.ArgumentMatchers.anyMap()
        );
        verify(monitoringService).recordAdminModerationAction(AdminActionType.REPORT_DELETE_TARGET, ReportTargetType.POST.name());
    }

    @Test
    @DisplayName("댓글 신고 대상 삭제 조치는 댓글을 삭제하고 신고 상태를 ACTION_TAKEN으로 변경한다.")
    void deleteTargetByReport_deletesCommentAndMarksActionTaken() {
        Report report = report(1L, ReportTargetType.COMMENT, 100L);
        Comment comment = Comment.createRoot(10L, 20L, "댓글 내용");
        ReflectionTestUtils.setField(comment, "id", 100L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(commentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(mock(Post.class)));

        ReportResponse response = service.deleteTargetByReport(1L, 99L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(response.status()).isEqualTo(ReportStatus.ACTION_TAKEN);
        assertThat(response.reviewerId()).isEqualTo(99L);
        verify(adminActionLogService).log(
                eq(99L),
                eq(AdminActionType.REPORT_DELETE_TARGET),
                eq(AdminActionTargetType.REPORT),
                eq(1L),
                eq("관리자가 신고 대상 삭제 조치를 완료했습니다."),
                org.mockito.ArgumentMatchers.anyMap()
        );
        verify(monitoringService).recordAdminModerationAction(AdminActionType.REPORT_DELETE_TARGET, ReportTargetType.COMMENT.name());
    }

    @Test
    @DisplayName("사용자 신고는 신고 대상 삭제 조치 API에서 지원하지 않는다.")
    void deleteTargetByReport_rejectsUserTarget() {
        Report report = report(1L, ReportTargetType.USER, 20L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.deleteTargetByReport(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNSUPPORTED_REPORT_TARGET);

        verify(postRepository, never()).findActiveById(20L);
        verify(commentRepository, never()).findByIdAndDeletedAtIsNull(20L);
    }

    @Test
    @DisplayName("없는 신고의 대상 삭제 조치는 REPORT_NOT_FOUND로 실패한다.")
    void deleteTargetByReport_rejectsMissingReport() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTargetByReport(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 삭제된 게시글 신고 대상 조치는 POST_NOT_FOUND로 실패하고 신고 상태를 바꾸지 않는다.")
    void deleteTargetByReport_rejectsAlreadyDeletedPost() {
        Report report = report(1L, ReportTargetType.POST, 10L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(postRepository.findActiveById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTargetByReport(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("USER 신고 정지 조치는 유저를 정지하고 신고 상태를 ACTION_TAKEN으로 변경한다.")
    void suspendUserByReport_suspendsUserAndMarksActionTaken() {
        Report report = report(1L, ReportTargetType.USER, 20L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportResponse response = service.suspendUserByReport(
                1L,
                99L,
                SuspensionType.TEMPORARY,
                SuspensionReason.SPAM,
                Duration.ofDays(7)
        );

        verify(adminUserCommandService).suspendUser(
                20L,
                SuspensionType.TEMPORARY,
                SuspensionReason.SPAM,
                Duration.ofDays(7)
        );
        assertThat(response.status()).isEqualTo(ReportStatus.ACTION_TAKEN);
        assertThat(response.reviewerId()).isEqualTo(99L);
        assertThat(response.reviewedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        verify(adminActionLogService).log(
                eq(99L),
                eq(AdminActionType.REPORT_SUSPEND_USER),
                eq(AdminActionTargetType.REPORT),
                eq(1L),
                eq("관리자가 USER 신고 대상 유저를 정지했습니다."),
                org.mockito.ArgumentMatchers.anyMap()
        );
        verify(monitoringService).recordAdminModerationAction(AdminActionType.REPORT_SUSPEND_USER, ReportTargetType.USER.name());
    }

    @Test
    @DisplayName("POST/COMMENT 신고는 USER 정지 조치 API에서 지원하지 않는다.")
    void suspendUserByReport_rejectsPostOrCommentTarget() {
        Report report = report(1L, ReportTargetType.POST, 10L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.suspendUserByReport(
                1L,
                99L,
                SuspensionType.PERMANENT,
                SuspensionReason.OTHER,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNSUPPORTED_REPORT_TARGET);

        verify(adminUserCommandService, never()).suspendUser(
                eq(10L),
                eq(SuspensionType.PERMANENT),
                eq(SuspensionReason.OTHER),
                eq(null)
        );
    }

    @Test
    @DisplayName("없는 신고로 USER 정지 조치를 요청하면 REPORT_NOT_FOUND로 실패한다.")
    void suspendUserByReport_rejectsMissingReport() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspendUserByReport(
                1L,
                99L,
                SuspensionType.PERMANENT,
                SuspensionReason.OTHER,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자는 신고 기반 조치로 자기 자신을 정지할 수 없다.")
    void suspendUserByReport_rejectsSelfSuspension() {
        Report report = report(1L, ReportTargetType.USER, 99L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.suspendUserByReport(
                1L,
                99L,
                SuspensionType.PERMANENT,
                SuspensionReason.OTHER,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_USER_STATUS);

        verify(adminUserCommandService, never()).suspendUser(
                eq(99L),
                eq(SuspensionType.PERMANENT),
                eq(SuspensionReason.OTHER),
                eq(null)
        );
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    private Report report(Long id, ReportTargetType targetType, Long targetId) {
        Report report = Report.create(targetType, targetId, 1L, "스팸", "신고 상세 내용");
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.now(CLOCK));
        return report;
    }
}
