package com.example.my_project_1.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static org.springframework.util.Assert.notNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "post_tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_tag_post_tag", columnNames = {"post_id", "tag_id"})
        }
)
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    public static PostTag create(Long postId, Long tagId) {
        notNull(postId, "게시글 ID는 필수입니다.");
        notNull(tagId, "태그 ID는 필수입니다.");

        PostTag postTag = new PostTag();
        postTag.postId = postId;
        postTag.tagId = tagId;
        return postTag;
    }
}
