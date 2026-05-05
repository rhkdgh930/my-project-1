package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentQueryServiceImpl implements CommentQueryService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserClient userClient;

    public List<CommentResponse> getComments(Long postId) {
        validatePost(postId);

        List<Comment> comments =
                commentRepository.findAllByPostIdOrderByIdAsc(postId);
        Map<Long, AuthorSummary> authorMap = findAuthors(postId, comments);

        Map<Long, CommentResponse> map = new HashMap<>();
        List<CommentResponse> roots = new ArrayList<>();

        for (Comment comment : comments) {
            CommentResponse response = CommentResponse.from(
                    comment,
                    authorMap.getOrDefault(comment.getUserId(), AuthorSummary.unknown())
            );
            map.put(comment.getId(), response);

            if (comment.getParentId() == null) {
                roots.add(response);
            } else {
                CommentResponse parent = map.get(comment.getParentId());
                if (parent != null) {
                    parent.addReply(response);
                }
            }
        }
        return roots;
    }

    private void validatePost(Long postId) {
        postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private Map<Long, AuthorSummary> findAuthors(Long postId, List<Comment> comments) {
        List<Long> authorIds = comments.stream()
                .filter(comment -> !comment.isDeleted())
                .map(Comment::getUserId)
                .distinct()
                .toList();

        if (authorIds.isEmpty()) {
            return Map.of();
        }

        try {
            return userClient.findAuthorsByIds(authorIds);
        } catch (RuntimeException e) {
            log.warn("[COMMENT_QUERY][AUTHOR_LOOKUP_FAIL] postId={} authorIds={}", postId, authorIds, e);
            return Map.of();
        }
    }
}
