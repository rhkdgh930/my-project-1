package com.example.my_project_1.user.repository;

import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(Email email);

    Boolean existsByEmailAndDeletedFalse(Email email);

    Optional<User> findByEmailAndDeletedFalse(Email email);
}
