package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Priority;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService.
 *
 * These are "unit" tests: the repositories are MOCKED, so no database and no
 * Spring context are needed. This makes them fast and lets us test one thing
 * at a time. Mockito's @Mock creates the fake repositories, and @InjectMocks
 * creates a real TaskService with those fakes injected.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");
        owner.setEmail("alice@example.com");
    }

    @Test
    void createTask_shouldSaveTaskAndReturnResponse() {
        // Arrange: the current user exists, and save() returns whatever it was given.
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest request = new TaskRequest(
                "Write report", "Quarterly report", LocalDateTime.now().plusDays(2),
                Priority.HIGH, null, null);

        // Act
        TaskResponse response = taskService.createTask(request, "alice");

        // Assert
        assertNotNull(response);
        assertEquals("Write report", response.title());
        assertEquals(Priority.HIGH, response.priority());
        assertEquals(TaskStatus.PENDING, response.status()); // default status
        assertEquals("alice", response.ownerUsername());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void getTask_whenTaskBelongsToAnotherUser_shouldThrowNotFound() {
        // Arrange: the repository finds nothing for this user+id combination.
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        // Act + Assert: we hide the fact the task exists at all -> 404.
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTask(99L, "alice"));
    }

    @Test
    void completeTask_shouldSetStatusToCompleted() {
        Task existing = new Task();
        existing.setId(5L);
        existing.setTitle("Buy milk");
        existing.setDueDate(LocalDateTime.now().plusDays(1));
        existing.setPriority(Priority.LOW);
        existing.setStatus(TaskStatus.PENDING);
        existing.setOwner(owner);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.completeTask(5L, "alice");

        assertEquals(TaskStatus.COMPLETED, response.status());
        verify(taskRepository).save(existing);
    }

    @Test
    void deleteTask_whenNotOwned_shouldThrowNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndOwnerId(7L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.deleteTask(7L, "alice"));
        verify(taskRepository, never()).delete(any(Task.class));
    }
}
