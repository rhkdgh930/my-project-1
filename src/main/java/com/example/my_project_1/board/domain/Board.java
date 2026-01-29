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
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE board SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "board")
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardStatus boardStatus;

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
        this.boardStatus = BoardStatus.ACTIVE;
    }

    public void update(String name, String description) {
        Assert.hasText(name, "게시판명은 필수입니다.");
        this.name = name;
        this.description = description;
    }

    public void delete() {
        if (boardStatus == BoardStatus.INACTIVE) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_BOARD);
        }
        boardStatus = BoardStatus.INACTIVE;
        super.softDelete();
    }
}
