package com.medigenius.repository;

import com.medigenius.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** NEW REPOSITORY (Feature 2 - User Entity). */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
