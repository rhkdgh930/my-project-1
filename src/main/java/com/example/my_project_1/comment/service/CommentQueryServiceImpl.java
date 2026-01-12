package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryServiceImpl implements CommentQueryService {
    private final CommentRepository commentRepository;

    public List<CommentResponse> getComments(Long postId) {
        List<Comment> comments =
                commentRepository.findAllByPostIdAndDeletedFalseOrderByIdAsc(postId);

        Map<Long, CommentResponse> map = new HashMap<>();
        List<CommentResponse> roots = new ArrayList<>();

        for (Comment comment : comments) {
            CommentResponse response = CommentResponse.from(comment);
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
}
