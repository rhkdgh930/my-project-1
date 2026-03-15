package com.example.my_project_1.board.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public void delete(LocalDateTime now) {
        if (super.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_BOARD);
        }
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        this.name = "deleted_" + uuid + "_" + this.name;
        this.description = "삭제된 게시판 입니다.";
        super.softDelete(now);
    }
}
