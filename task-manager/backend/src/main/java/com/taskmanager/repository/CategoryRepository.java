package com.taskmanager.repository;

import com.taskmanager.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // All categories belonging to one user.
    List<Category> findByOwnerId(Long ownerId);

    // Fetch a category only if it belongs to this user (used for ownership checks).
    Optional<Category> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByNameAndOwnerId(String name, Long ownerId);
}
