package com.example.my_project_1.board.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "board")
public class Board extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        this.updatedAt = LocalDateTime.now();
    }
}
