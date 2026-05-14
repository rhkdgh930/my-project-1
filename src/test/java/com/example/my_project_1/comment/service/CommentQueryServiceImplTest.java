package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.comment.service.impl.CommentQueryServiceImpl;
import com.example.my_project_1.comment.service.response.CommentResponse;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.user.client.AuthorStatus;
import com.example.my_project_1.user.client.AuthorSummary;
import com.example.my_project_1.user.client.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentQueryServiceImplTest {

    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private UserClient userClient;
    private CommentQueryServiceImpl commentQueryService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        userClient = mock(UserClient.class);
        commentQueryService = new CommentQueryServiceImpl(commentRepository, postRepository, userClient);
    }

    @Test
    @DisplayName("댓글 트리 조회는 삭제된 부모 댓글도 포함해 자식 대댓글을 유지한다.")
    void getComments_keepsChildrenUnderDeletedParent() {
        Long postId = 1L;
        Comment parent = comment(postId, 100L, 10L, "parent", null, 0);
        Comment child = comment(postId, 101L, 20L, "reply", 100L, 1);
        parent.delete(10L, LocalDateTime.of(2026, 5, 1, 0, 0));

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(mock(Post.class)));
        when(commentRepository.findAllByPostIdOrderByIdAsc(postId))
                .thenReturn(List.of(parent, child));
        when(userClient.findAuthorsByIds(List.of(20L)))
                .thenReturn(Map.of(20L, AuthorSummary.active(20L, "reply-author", "/images/reply.png")));

        List<CommentResponse> responses = commentQueryService.getComments(postId);

        assertThat(responses).hasSize(1);
        CommentResponse parentResponse = responses.get(0);
        assertThat(parentResponse.isDeleted()).isTrue();
        assertThat(parentResponse.getAuthorId()).isNull();
        assertThat(parentResponse.getAuthor()).isNull();
        assertThat(parentResponse.getContent()).isEqualTo(Comment.DELETED_CONTENT);
        assertThat(parentResponse.getReplies()).hasSize(1);
        assertThat(parentResponse.getReplies().get(0).isDeleted()).isFalse();
        assertThat(parentResponse.getReplies().get(0).getAuthorId()).isEqualTo(20L);
        assertThat(parentResponse.getReplies().get(0).getAuthor().id()).isEqualTo(20L);
        assertThat(parentResponse.getReplies().get(0).getAuthor().displayName()).isEqualTo("reply-author");
        assertThat(parentResponse.getReplies().get(0).getAuthor().status()).isEqualTo(AuthorStatus.ACTIVE);
        assertThat(parentResponse.getReplies().get(0).getAuthor().profileImageUrl()).isEqualTo("/images/reply.png");
        assertThat(parentResponse.getReplies().get(0).getContent()).isEqualTo("reply");
        verify(commentRepository).findAllByPostIdOrderByIdAsc(postId);
    }

    @Test
    @DisplayName("댓글 트리 조회는 일반 댓글과 대댓글 author를 채운다.")
    void getComments_fillsRootAndReplyAuthors() {
        Long postId = 1L;
        Comment parent = comment(postId, 100L, 10L, "parent", null, 0);
        Comment child = comment(postId, 101L, 20L, "reply", 100L, 1);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(mock(Post.class)));
        when(commentRepository.findAllByPostIdOrderByIdAsc(postId))
                .thenReturn(List.of(parent, child));
        when(userClient.findAuthorsByIds(List.of(10L, 20L)))
                .thenReturn(Map.of(
                        10L, AuthorSummary.active(10L, "parent-author", "/images/parent.png"),
                        20L, AuthorSummary.suspended(20L, "reply-author")
                ));

        List<CommentResponse> responses = commentQueryService.getComments(postId);

        CommentResponse parentResponse = responses.get(0);
        CommentResponse replyResponse = parentResponse.getReplies().get(0);

        assertThat(parentResponse.getAuthorId()).isEqualTo(10L);
        assertThat(parentResponse.getAuthor().id()).isEqualTo(10L);
        assertThat(parentResponse.getAuthor().displayName()).isEqualTo("parent-author");
        assertThat(parentResponse.getAuthor().status()).isEqualTo(AuthorStatus.ACTIVE);
        assertThat(parentResponse.getAuthor().profileImageUrl()).isEqualTo("/images/parent.png");
        assertThat(replyResponse.getAuthorId()).isEqualTo(20L);
        assertThat(replyResponse.getAuthor().id()).isEqualTo(20L);
        assertThat(replyResponse.getAuthor().displayName()).isEqualTo("차단된 사용자");
        assertThat(replyResponse.getAuthor().status()).isEqualTo(AuthorStatus.SUSPENDED);
        assertThat(replyResponse.getAuthor().profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("댓글 트리 조회는 작성자 조회 실패 시 UNKNOWN author fallback을 사용한다.")
    void getComments_usesUnknownAuthorWhenUserLookupFails() {
        Long postId = 1L;
        Comment comment = comment(postId, 100L, 10L, "comment", null, 0);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(mock(Post.class)));
        when(commentRepository.findAllByPostIdOrderByIdAsc(postId))
                .thenReturn(List.of(comment));
        when(userClient.findAuthorsByIds(List.of(10L))).thenReturn(Map.of());

        List<CommentResponse> responses = commentQueryService.getComments(postId);

        CommentResponse response = responses.get(0);
        assertThat(response.getAuthorId()).isEqualTo(10L);
        assertThat(response.getAuthor().id()).isNull();
        assertThat(response.getAuthor().displayName()).isEqualTo("알 수 없는 사용자");
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("comment list는 author bulk lookup 실패 시 UNKNOWN author를 사용한다.")
    void getComments_usesUnknownAuthorWhenUserLookupThrows() {
        Long postId = 1L;
        Comment parent = comment(postId, 100L, 10L, "parent", null, 0);
        Comment child = comment(postId, 101L, 20L, "reply", 100L, 1);
        parent.delete(10L, LocalDateTime.of(2026, 5, 1, 0, 0));

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(mock(Post.class)));
        when(commentRepository.findAllByPostIdOrderByIdAsc(postId))
                .thenReturn(List.of(parent, child));
        when(userClient.findAuthorsByIds(List.of(20L)))
                .thenThrow(new RuntimeException("user lookup failed"));

        List<CommentResponse> responses = commentQueryService.getComments(postId);

        CommentResponse parentResponse = responses.get(0);
        CommentResponse replyResponse = parentResponse.getReplies().get(0);
        assertThat(parentResponse.isDeleted()).isTrue();
        assertThat(parentResponse.getAuthorId()).isNull();
        assertThat(parentResponse.getAuthor()).isNull();
        assertThat(replyResponse.isDeleted()).isFalse();
        assertThat(replyResponse.getAuthorId()).isEqualTo(20L);
        assertThat(replyResponse.getAuthor().id()).isNull();
        assertThat(replyResponse.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("댓글 트리 조회는 active post만 허용한다.")
    void getComments_requiresActivePost() {
        Long postId = 1L;
        when(postRepository.findActiveById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentQueryService.getComments(postId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(commentRepository, never()).findAllByPostIdOrderByIdAsc(postId);
    }

    private static Comment comment(Long postId, Long commentId, Long userId, String content, Long parentId, int depth) {
        Comment comment = Comment.createRoot(postId, userId, content);
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "parentId", parentId);
        ReflectionTestUtils.setField(comment, "depth", depth);
        return comment;
    }
}
