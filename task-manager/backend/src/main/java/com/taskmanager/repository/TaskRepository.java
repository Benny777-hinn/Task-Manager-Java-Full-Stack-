package com.taskmanager.repository;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TaskRepository.
 *
 * We also extend JpaSpecificationExecutor so the service can build a *dynamic*
 * WHERE clause for filters (status/priority/category/keyword) at runtime.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // Ownership check: find a task only if it belongs to this user.
    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    List<Task> findByCategoryId(Long categoryId);

    /**
     * Used by the hourly reminder scheduler.
     * Returns PENDING tasks that are due in the given window and not yet reminded.
     * JOIN FETCH t.owner loads the owner in the same query, so we can read the
     * owner's email without a lazy-loading error.
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.owner " +
            "WHERE t.status = :status " +
            "AND t.reminderSent = false " +
            "AND t.dueDate BETWEEN :from AND :to")
    List<Task> findTasksDueForReminder(@Param("status") TaskStatus status,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);
}
