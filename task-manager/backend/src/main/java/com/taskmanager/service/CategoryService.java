package com.taskmanager.service;

import com.taskmanager.dto.CategoryRequest;
import com.taskmanager.dto.CategoryResponse;
import com.taskmanager.entity.Category;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for categories. Categories are always scoped to the owner.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TaskRepository taskRepository,
                           UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, String username) {
        User owner = getCurrentUser(username);

        // One user cannot create two categories with the same name.
        if (categoryRepository.existsByNameAndOwnerId(request.name(), owner.getId())) {
            throw new ConflictException("You already have a category named '" + request.name() + "'");
        }

        Category category = new Category(request.name(), owner);
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getId(), saved.getName(), owner.getId());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(String username) {
        User owner = getCurrentUser(username);
        return categoryRepository.findByOwnerId(owner.getId()).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), owner.getId()))
                .toList();
    }

    @Transactional
    public void deleteCategory(Long id, String username) {
        User owner = getCurrentUser(username);
        Category category = categoryRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));

        // Tasks that used this category must not break: set their category to null
        // before removing the category (otherwise the foreign key would block us).
        List<Task> tasks = taskRepository.findByCategoryId(category.getId());
        for (Task task : tasks) {
            task.setCategory(null);
        }
        taskRepository.saveAll(tasks);

        categoryRepository.delete(category);
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
