package com.example.my_project_1.user.repository;

import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(Email email);

    Optional<User> findByEmail(Email email);

    @Query(value = "SELECT * FROM users", nativeQuery = true)
    List<User> findAllRaw();
}
