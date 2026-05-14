package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.request.PostSearchType;
import com.example.my_project_1.post.service.request.PostSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.my_project_1.post.domain.QPost.post;

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

    private BooleanExpression boardIdEq(Long boardId) {
        return boardId != null ? post.board.id.eq(boardId) : null;
    }

    private BooleanExpression activePost() {
        return post.deletedAt.isNull()
                .and(post.board.deletedAt.isNull());
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
