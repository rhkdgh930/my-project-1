package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.comment.service.response.CommentResponse;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
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
    private CommentQueryServiceImpl commentQueryService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        commentQueryService = new CommentQueryServiceImpl(commentRepository, postRepository);
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

        List<CommentResponse> responses = commentQueryService.getComments(postId);

        assertThat(responses).hasSize(1);
        CommentResponse parentResponse = responses.get(0);
        assertThat(parentResponse.isDeleted()).isTrue();
        assertThat(parentResponse.getAuthorId()).isNull();
        assertThat(parentResponse.getContent()).isEqualTo(Comment.DELETED_CONTENT);
        assertThat(parentResponse.getReplies()).hasSize(1);
        assertThat(parentResponse.getReplies().get(0).isDeleted()).isFalse();
        assertThat(parentResponse.getReplies().get(0).getAuthorId()).isEqualTo(20L);
        assertThat(parentResponse.getReplies().get(0).getContent()).isEqualTo("reply");
        verify(commentRepository).findAllByPostIdOrderByIdAsc(postId);
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
