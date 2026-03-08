package com.example.my_project_1.board.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [Policy] 이 엔티티는 Soft Delete 정책을 따릅니다.
 * 삭제 시 실제 DELETE 대신 @SQLDelete에 정의된 UPDATE가 실행되며,
 * @SQLRestriction에 의해 삭제되지 않은 데이터만 기본 조회됩니다.
 */

@SQLDelete(sql = "UPDATE board SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "board")
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    public static Board create(String name, String description) {
        return Board.builder()
                .name(name)
                .description(description)
                .build();
    }

    @Builder
    private Board(String name, String description) {
        Assert.hasText(name, "게시판명은 필수입니다.");
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description) {
        Assert.hasText(name, "게시판명은 필수입니다.");
        this.name = name;
        this.description = description;
    }

    public void maskBoardData(String uuid) {
        this.name = this.name + "_deleted_" + uuid;
        this.description = "삭제된 게시판 입니다.";
    }

}
