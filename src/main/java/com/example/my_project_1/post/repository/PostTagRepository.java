package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @Modifying
    void deleteByPostId(Long postId);

    @Query("""
            select pt.postId as postId,
                   t.name as name
            from PostTag pt
            join Tag t on pt.tagId = t.id
            where pt.postId in :postIds
            order by pt.id asc
            """)
    List<PostTagNameProjection> findTagNamesByPostIdIn(@Param("postIds") Collection<Long> postIds);
}
