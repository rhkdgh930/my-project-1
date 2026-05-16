package com.example.my_project_1.admin.repository;

import com.example.my_project_1.admin.domain.AdminActionLog;
import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.request.AdminActionLogSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.my_project_1.admin.domain.QAdminActionLog.adminActionLog;

@RequiredArgsConstructor
public class AdminActionLogRepositoryImpl implements AdminActionLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdminActionLog> searchLogs(AdminActionLogSearchCondition condition, Pageable pageable) {
        AdminActionLogSearchCondition searchCondition =
                condition != null ? condition : new AdminActionLogSearchCondition(null, null, null);

        List<AdminActionLog> content = queryFactory
                .selectFrom(adminActionLog)
                .where(
                        actionTypeEq(searchCondition.actionType()),
                        targetTypeEq(searchCondition.targetType()),
                        adminIdEq(searchCondition.adminId())
                )
                .orderBy(
                        adminActionLog.createdAt.desc(),
                        adminActionLog.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(adminActionLog.count())
                .from(adminActionLog)
                .where(
                        actionTypeEq(searchCondition.actionType()),
                        targetTypeEq(searchCondition.targetType()),
                        adminIdEq(searchCondition.adminId())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression actionTypeEq(AdminActionType actionType) {
        return actionType != null ? adminActionLog.actionType.eq(actionType) : null;
    }

    private BooleanExpression targetTypeEq(AdminActionTargetType targetType) {
        return targetType != null ? adminActionLog.targetType.eq(targetType) : null;
    }

    private BooleanExpression adminIdEq(Long adminId) {
        return adminId != null ? adminActionLog.adminId.eq(adminId) : null;
    }
}
