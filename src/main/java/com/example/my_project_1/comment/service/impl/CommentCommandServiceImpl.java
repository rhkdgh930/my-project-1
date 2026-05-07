package com.example.my_project_1.comment.service.impl;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.comment.service.CommentCommandService;
import com.example.my_project_1.comment.service.response.CommentResponse;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.user.client.AuthorSummary;
import com.example.my_project_1.user.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
@RequiredArgsConstructor
@Slf4j
@Service
public class CommentCommandServiceImpl implements CommentCommandService {
    private final Clock clock;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserClient userClient;

    public Long writeComment(Long postId, Long userId, String content) {
        validatePost(postId);
        Comment comment = Comment.createRoot(postId, userId, content);
        return commentRepository.save(comment).getId();
    }

    public Long writeReply(Long postId, Long parentId, Long userId, String content) {
        Comment parent = getComment(parentId);
        validateParentPost(postId, parent);
        validatePost(postId);

        Comment reply = Comment.createReply(parent, userId, content);
        return commentRepository.save(reply).getId();
    }

    public CommentResponse update(Long postId, Long commentId, Long userId, String content) {
        Comment comment = getComment(commentId);
        validateCommentPost(postId, comment);
        validatePost(postId);
        comment.updateContent(content, userId);
        return CommentResponse.from(comment, getAuthorOrUnknown(comment.getUserId()));
    }

    public void delete(Long postId, Long commentId, Long userId) {
        Comment comment = getComment(commentId);
        validateCommentPost(postId, comment);
        validatePost(postId);
        comment.delete(userId, LocalDateTime.now(clock));
    }

    private Comment getComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validatePost(Long postId) {
        postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateParentPost(Long postId, Comment parent) {
        validateCommentPost(postId, parent);
    }

    private void validateCommentPost(Long postId, Comment comment) {
        if (!comment.getPostId().equals(postId)) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_POST_RELATION);
        }
    }

    private AuthorSummary getAuthorOrUnknown(Long userId) {
        try {
            return userClient.findAuthorsByIds(List.of(userId))
                    .getOrDefault(userId, AuthorSummary.unknown());
        } catch (RuntimeException e) {
            log.warn("[COMMENT_COMMAND][AUTHOR_LOOKUP_FAIL] userId={}", userId, e);
            return AuthorSummary.unknown();
        }
    }
}
