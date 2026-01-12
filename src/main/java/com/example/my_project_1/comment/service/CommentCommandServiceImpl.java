package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class CommentCommandServiceImpl implements CommentCommandService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public Long writeComment(Long postId, Long authorId, String content) {
        validatePost(postId);
        Comment comment = Comment.createRoot(postId, authorId, content);
        return commentRepository.save(comment).getId();
    }

    public Long writeReply(Long parentId, Long authorId, String content) {
        Comment parent = commentRepository.findByIdAndDeletedFalse(parentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 없습니다."));

        Comment reply = Comment.createReply(parent, authorId, content);
        return commentRepository.save(reply).getId();
    }

    public void update(Long commentId, Long userId, String content) {
        Comment comment = getComment(commentId);
        comment.updateContent(content, userId);
    }

    public void delete(Long commentId, Long userId) {
        Comment comment = getComment(commentId);
        comment.delete(userId);
    }

    private Comment getComment(Long id) {
        return commentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다."));
    }

    private void validatePost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("게시글이 존재하지 않습니다.");
        }
    }
}
