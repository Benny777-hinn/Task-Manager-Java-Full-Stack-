package com.taskmanager.service;

import com.taskmanager.dto.TaskResponse;
import com.taskmanager.dto.UserResponse;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-only operations. Keeping them in their own service makes the permission
 * boundary obvious: only AdminController calls this class.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TaskService taskService;

    public AdminService(UserRepository userRepository, TaskService taskService) {
        this.userRepository = userRepository;
        this.taskService = taskService;
    }

    /** Page through every registered user. */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Delete a user. Because User has cascade = ALL + orphanRemoval on tasks and
     * categories, their data is removed as well.
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        userRepository.delete(user);
    }

    /** Admins can see every task in the system. */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        // Reuse TaskService so the JSON shape stays identical to the normal endpoints.
        return taskService.getAllTasksForAdmin(pageable);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.getCreatedAt());
    }
}
