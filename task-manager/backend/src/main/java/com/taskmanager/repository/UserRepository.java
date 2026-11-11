package com.taskmanager.repository;

import com.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA generates the implementation of this interface for us.
 * We only declare the query methods we need; Spring writes the SQL.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used at login and to load the current user from the JWT.
    Optional<User> findByUsername(String username);

    // Used at registration to give a friendly "already taken" error.
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
