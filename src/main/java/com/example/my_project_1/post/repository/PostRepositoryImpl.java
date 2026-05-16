package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.request.PostSearchType;
import com.example.my_project_1.post.service.request.PostSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.my_project_1.comment.domain.QComment.comment;
import static com.example.my_project_1.post.domain.QPostLike.postLike;
import static com.example.my_project_1.post.domain.QPostTag.postTag;
import static com.example.my_project_1.post.domain.QPost.post;
import static com.example.my_project_1.post.domain.QTag.tag;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> searchActivePosts(
            Long boardId,
            PostSearchCondition condition,
            Pageable pageable
    ) {
        PostSearchCondition searchCondition =
                condition != null ? condition : new PostSearchCondition();

        List<Post> content = queryFactory
                .selectFrom(post)
                .where(
                        boardIdEq(boardId),
                        activePost(),
                        search(searchCondition)
                )
                .orderBy(orderSpecifiers(searchCondition.sortTypeOrDefault()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        boardIdEq(boardId),
                        activePost(),
                        search(searchCondition)
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    @Override
    public Page<Post> findLikedActivePostsByUserId(Long userId, Pageable pageable) {
        List<Post> content = queryFactory
                .select(post)
                .from(postLike)
                .join(post).on(postLike.postId.eq(post.id))
                .where(
                        postLike.userId.eq(userId),
                        activePost()
                )
                .orderBy(
                        postLike.createdAt.desc(),
                        post.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(postLike)
                .join(post).on(postLike.postId.eq(post.id))
                .where(
                        postLike.userId.eq(userId),
                        activePost()
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    @Override
    public Page<Post> findActivePostsByUserId(Long userId, Pageable pageable) {
        List<Post> content = queryFactory
                .selectFrom(post)
                .where(
                        post.userId.eq(userId),
                        activePost()
                )
                .orderBy(
                        post.createdAt.desc(),
                        post.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.userId.eq(userId),
                        activePost()
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    @Override
    public Page<Post> findCommentedActivePostsByUserId(Long userId, Pageable pageable) {
        List<Long> postIds = queryFactory
                .select(post.id)
                .from(comment)
                .join(post).on(comment.postId.eq(post.id))
                .where(
                        comment.userId.eq(userId),
                        activeComment(),
                        activePost()
                )
                .groupBy(post.id)
                .orderBy(
                        comment.createdAt.max().desc(),
                        post.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.postId.countDistinct())
                .from(comment)
                .join(post).on(comment.postId.eq(post.id))
                .where(
                        comment.userId.eq(userId),
                        activeComment(),
                        activePost()
                )
                .fetchOne();

        if (postIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total != null ? total : 0L);
        }

        Map<Long, Post> postMap = queryFactory
                .selectFrom(post)
                .where(post.id.in(postIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<Post> content = postIds.stream()
                .map(postMap::get)
                .toList();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    @Override
    public Page<Post> findActivePostsByTagName(String tagName, Pageable pageable) {
        String normalizedTagName = tagName != null ? tagName.trim() : "";
        if (normalizedTagName.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Post> content = queryFactory
                .select(post)
                .from(postTag)
                .join(tag).on(postTag.tagId.eq(tag.id))
                .join(post).on(postTag.postId.eq(post.id))
                .where(
                        tag.name.eq(normalizedTagName),
                        activePost()
                )
                .orderBy(
                        post.createdAt.desc(),
                        post.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(postTag)
                .join(tag).on(postTag.tagId.eq(tag.id))
                .join(post).on(postTag.postId.eq(post.id))
                .where(
                        tag.name.eq(normalizedTagName),
                        activePost()
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    @Override
    public List<Post> findPopularActivePosts(Long boardId, int size) {
        NumberExpression<Long> score = post.likeCount.multiply(3L).add(post.viewCount);

        return queryFactory
                .selectFrom(post)
                .where(
                        boardIdEq(boardId),
                        activePost()
                )
                .orderBy(
                        score.desc(),
                        post.id.desc()
                )
                .limit(size)
                .fetch();
    }

    private BooleanExpression boardIdEq(Long boardId) {
        return boardId != null ? post.board.id.eq(boardId) : null;
    }

    private BooleanExpression activePost() {
        return post.deletedAt.isNull()
                .and(post.board.deletedAt.isNull());
    }

    private BooleanExpression activeComment() {
        return comment.deletedAt.isNull();
    }

    private BooleanExpression search(PostSearchCondition condition) {
        if (condition == null || !condition.hasKeyword()) {
            return null;
        }

        String keyword = condition.normalizedKeyword();
        PostSearchType searchType = condition.searchTypeOrDefault();

        return switch (searchType) {
            case TITLE -> post.title.containsIgnoreCase(keyword);
            case CONTENT -> contentAsString().containsIgnoreCase(keyword);
            case TITLE_CONTENT -> post.title.containsIgnoreCase(keyword)
                    .or(contentAsString().containsIgnoreCase(keyword));
        };
    }

    private StringExpression contentAsString() {
        return Expressions.stringTemplate("cast({0} as string)", post.content);
    }

    private OrderSpecifier<?>[] orderSpecifiers(PostSortType sortType) {
        PostSortType type = sortType != null ? sortType : PostSortType.LATEST;

        return switch (type) {
            case LATEST -> new OrderSpecifier[]{
                    post.createdAt.desc(),
                    post.id.desc()
            };
            case OLDEST -> new OrderSpecifier[]{
                    post.createdAt.asc(),
                    post.id.asc()
            };
            case VIEW_COUNT -> new OrderSpecifier[]{
                    post.viewCount.desc(),
                    post.id.desc()
            };
            case LIKE_COUNT -> new OrderSpecifier[]{
                    post.likeCount.desc(),
                    post.id.desc()
            };
        };
    }
}
