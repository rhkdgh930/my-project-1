package com.example.my_project_1.report.service.impl;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.AdminActionLogService;
import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.report.domain.Report;
import com.example.my_project_1.report.domain.ReportStatus;
import com.example.my_project_1.report.domain.ReportTargetType;
import com.example.my_project_1.report.repository.ReportRepository;
import com.example.my_project_1.report.service.request.ReportCreateRequest;
import com.example.my_project_1.report.service.request.ReportStatusUpdateRequest;
import com.example.my_project_1.report.service.response.ReportResponse;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-15T01:02:03Z"),
            ZoneId.of("Asia/Seoul")
    );

    private ReportRepository reportRepository;
    private PostRepository postRepository;
    private CommentRepository commentRepository;
    private UserRepository userRepository;
    private AdminActionLogService adminActionLogService;
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        postRepository = mock(PostRepository.class);
        commentRepository = mock(CommentRepository.class);
        userRepository = mock(UserRepository.class);
        adminActionLogService = mock(AdminActionLogService.class);
        reportService = new ReportServiceImpl(
                reportRepository,
                postRepository,
                commentRepository,
                userRepository,
                adminActionLogService,
                CLOCK
        );
    }

    @Test
    @DisplayName("게시글 신고는 active post를 검증하고 PENDING 신고를 저장한다.")
    void createPostReport_savesPendingReport() {
        ReportCreateRequest request = request(ReportTargetType.POST, 10L);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(mock(Post.class)));
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 1L);
            return report;
        });

        ReportResponse response = reportService.create(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.targetType()).isEqualTo(ReportTargetType.POST);
        assertThat(response.targetId()).isEqualTo(10L);
        assertThat(response.reporterId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("댓글 신고는 active comment와 해당 게시글의 active 상태를 검증한다.")
    void createCommentReport_validatesCommentAndPost() {
        Comment comment = Comment.createRoot(10L, 2L, "댓글 내용");
        ReportCreateRequest request = request(ReportTargetType.COMMENT, 100L);
        when(commentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(mock(Post.class)));
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.create(1L, request);

        assertThat(response.targetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("유저 신고는 target user 존재 여부를 검증하고 자기 자신 신고는 제외한다.")
    void createUserReport_savesReportForOtherUser() {
        ReportCreateRequest request = request(ReportTargetType.USER, 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.create(1L, request);

        assertThat(response.targetType()).isEqualTo(ReportTargetType.USER);
        assertThat(response.targetId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("같은 사용자가 같은 대상을 중복 신고하면 실패한다.")
    void createReport_rejectsDuplicatedReport() {
        ReportCreateRequest request = request(ReportTargetType.POST, 10L);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(mock(Post.class)));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATED_REPORT);

        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("unique 제약 충돌도 중복 신고 실패로 변환한다.")
    void createReport_convertsUniqueConflictToDuplicatedReport() {
        ReportCreateRequest request = request(ReportTargetType.POST, 10L);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(mock(Post.class)));
        when(reportRepository.saveAndFlush(any(Report.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATED_REPORT);
    }

    @Test
    @DisplayName("삭제됐거나 없는 게시글 신고는 실패한다.")
    void createReport_rejectsDeletedOrMissingPost() {
        ReportCreateRequest request = request(ReportTargetType.POST, 10L);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제됐거나 없는 댓글 신고는 실패한다.")
    void createReport_rejectsDeletedOrMissingComment() {
        ReportCreateRequest request = request(ReportTargetType.COMMENT, 100L);
        when(commentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 유저 신고는 실패한다.")
    void createReport_rejectsMissingUser() {
        ReportCreateRequest request = request(ReportTargetType.USER, 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("자기 자신 유저 신고는 실패한다.")
    void createReport_rejectsSelfUserReport() {
        ReportCreateRequest request = request(ReportTargetType.USER, 1L);

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SELF_REPORT_NOT_ALLOWED);
    }

    @Test
    @DisplayName("관리자는 신고 목록을 페이지로 조회한다.")
    void findReports_returnsPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        Report report = report(1L, ReportTargetType.POST, 10L, 1L);
        when(reportRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(report), pageable, 1));

        PageResponse<ReportResponse> response = reportService.findReports(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("관리자는 신고 상세를 조회한다.")
    void findReport_returnsDetail() {
        Report report = report(1L, ReportTargetType.POST, 10L, 1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportResponse response = reportService.findReport(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("신고 상세 내용");
    }

    @Test
    @DisplayName("없는 신고 상세 조회는 REPORT_NOT_FOUND로 실패한다.")
    void findReport_rejectsMissingReport() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.findReport(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자는 신고 상태를 변경하고 검토자와 검토 시각을 기록한다.")
    void updateStatus_updatesReviewMetadata() {
        Report report = report(1L, ReportTargetType.POST, 10L, 1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportResponse response = reportService.updateStatus(
                1L,
                99L,
                new ReportStatusUpdateRequest(ReportStatus.REVIEWED)
        );

        assertThat(response.status()).isEqualTo(ReportStatus.REVIEWED);
        assertThat(response.reviewerId()).isEqualTo(99L);
        assertThat(response.reviewedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        verify(adminActionLogService).log(
                org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.eq(AdminActionType.REPORT_STATUS_CHANGE),
                org.mockito.ArgumentMatchers.eq(AdminActionTargetType.REPORT),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("관리자가 신고 상태를 변경했습니다."),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }

    @Test
    @DisplayName("신고 상태를 ACTION_TAKEN으로 변경해도 대상 게시글이나 댓글을 변경하지 않는다.")
    void updateStatusToActionTaken_doesNotModerateTarget() {
        Report report = report(1L, ReportTargetType.POST, 10L, 1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportResponse response = reportService.updateStatus(
                1L,
                99L,
                new ReportStatusUpdateRequest(ReportStatus.ACTION_TAKEN)
        );

        assertThat(response.status()).isEqualTo(ReportStatus.ACTION_TAKEN);
        verify(postRepository, never()).findActiveById(any());
        verify(commentRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    private ReportCreateRequest request(ReportTargetType targetType, Long targetId) {
        return new ReportCreateRequest(targetType, targetId, "스팸", "신고 상세 내용");
    }

    private Report report(Long id, ReportTargetType targetType, Long targetId, Long reporterId) {
        Report report = Report.create(targetType, targetId, reporterId, "스팸", "신고 상세 내용");
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.now(CLOCK));
        return report;
    }

    private User user(Long id) {
        User user = User.signUp(
                Email.from("user" + id + "@example.com"),
                "encoded-password",
                "사용자" + id,
                LocalDateTime.now(CLOCK)
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
