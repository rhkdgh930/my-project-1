package com.example.my_project_1.user.repository;

import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(Email email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(Email email);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id > :lastId
        ORDER BY u.id ASC
        """)
    List<User> findNextUsers(
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    @Query("""
                SELECT u
                FROM User u
                WHERE u.id > :lastId
                  AND u.userStatus = :status
                  AND u.lastLoginAt <= :threshold
                ORDER BY u.id ASC
            """)
    List<User> findDormantUsers(
            @Param("lastId") Long lastId,
            @Param("status") UserStatus status,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable
    );

    @Query("""
                SELECT u
                FROM User u
                WHERE u.id > :lastId
                  AND u.userStatus = :status
                  AND u.withdrawal.requestedAt <= :threshold
                ORDER BY u.id ASC
            """)
    List<User> findWithdrawalUsers(
            @Param("lastId") Long lastId,
            @Param("status") UserStatus status,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable
    );
}
