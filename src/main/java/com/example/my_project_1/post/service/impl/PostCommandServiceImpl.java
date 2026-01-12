package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.domain.BoardStatus;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.postimage.domain.ImageStatus;
import com.example.my_project_1.postimage.domain.PostImage;
import com.example.my_project_1.postimage.repository.PostImageRepository;
import com.example.my_project_1.postimage.utils.ImageUrlParser;
import com.example.my_project_1.user.client.UserClient;
import com.example.my_project_1.user.client.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Transactional
@RequiredArgsConstructor
@Service
public class PostCommandServiceImpl implements PostCommandService {
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostRedisService postRedisService;
    private final UserClient userClient;

    @Override
    public PostDetailResponse create(Long boardId, Long userId, PostCreateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판 없음"));

        String nickname = getNickname(userId);

        Post post = Post.create(
                board,
                userId,
                request.getTitle(),
                request.getContent()
        );

        attachImages(post, userId, request.getContent());

        return PostDetailResponse.from(postRepository.save(post), nickname);
    }

    private String getNickname(Long userId) {
        Map<Long, UserSummary> userMap = userClient.findUsersByIds(List.of(userId));
        UserSummary user = userMap.get(userId);
        return (user != null) ? user.nickname() : "알 수 없는 사용자";
    }

    private void attachImages(Post post, Long userId, String content) {
        List<String> urls = ImageUrlParser.extract(content);
        List<PostImage> images =
                postImageRepository.findAllByImageUrlInAndUploaderId(urls, userId);

        images.forEach(img -> img.attach(post));
    }

    @Override
    public PostDetailResponse update(Long boardId, Long postId, Long userId, PostUpdateRequest request) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.INVALID_BOARD_POST_RELATION);
        }
        if (post.getBoard().getBoardStatus() == BoardStatus.INACTIVE) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_BOARD);
        }

        if (!post.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        String nickname = getNickname(userId);
        syncImages(post, userId, request.getContent());

        post.update(request.getTitle(), request.getContent());
        return PostDetailResponse.from(post, nickname);
    }

    private void syncImages(Post post, Long userId, String content) {
        List<String> currentUrls = ImageUrlParser.extract(content);

        List<PostImage> toRemove = post.getImages().stream()
                .filter(img -> img.getImageStatus() == ImageStatus.USED)
                .filter(img -> !currentUrls.contains(img.getImageUrl()))
                .toList();

        toRemove.forEach(post::removeImage);

        List<PostImage> newImages =
                postImageRepository.findAllByImageUrlInAndUploaderId(
                        currentUrls, userId
                );

        newImages.stream()
                .filter(img -> img.getImageStatus() == ImageStatus.PENDING)
                .forEach(img -> img.attach(post));
    }

    @Override
    public boolean like(Long boardId, Long postId, Long userId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.INVALID_BOARD_POST_RELATION);
        }

        return postRedisService.toggleLike(postId, userId);
    }
}