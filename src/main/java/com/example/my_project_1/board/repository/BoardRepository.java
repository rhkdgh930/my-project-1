package com.example.my_project_1.board.repository;

import com.example.my_project_1.board.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    boolean existsByName(String name);

    Optional<Board> findByIdAndDeletedAtIsNull(Long boardId);

    List<Board> findAllByDeletedAtIsNull();
}
