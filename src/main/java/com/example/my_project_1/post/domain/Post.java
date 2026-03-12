package com.example.my_project_1.post.domain;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.postimage.domain.PostImage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.notNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post")
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    private Long userId;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL
    )
    private List<PostImage> images = new ArrayList<>();

    private long viewCount;
    private long likeCount;

    public static Post create(Board board, Long userId, String title, String content) {
        return Post.builder()
                .board(board)
                .userId(userId)
                .title(title)
                .content(content)
                .build();
    }

    @Builder
    private Post(Board board, Long userId, String title, String content) {
        notNull(board, "게시판 정보는 필수입니다.");
        notNull(userId, "작성자 ID는 필수입니다.");
        hasText(title, "제목은 필수입니다.");
        hasText(content, "내용은 필수입니다.");

        this.board = board;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.viewCount = 0L;
        this.likeCount = 0L;
    }

    public void update(String title, String content) {
        hasText(title, "제목은 필수입니다.");
        hasText(content, "내용은 필수입니다.");
        this.title = title;
        this.content = content;
    }

    public void delete(LocalDateTime now) {
        if (super.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_POST);
        }
        this.title = "삭제된 게시글입니다.";
        this.content = "삭제된 게시글입니다.";
        super.softDelete(now);
    }

    public void addImage(PostImage image) {
        if (this.images.contains(image)) {
            return;
        }
        images.add(image);
        image.attach(this);
    }

    public void removeImage(PostImage image) {
        images.remove(image);
        image.detach();
    }

    public void updateCounts(long viewCount, long likeCount) {
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }
}
