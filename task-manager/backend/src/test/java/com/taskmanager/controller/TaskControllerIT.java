package com.taskmanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.RegisterRequest;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.entity.Priority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the task endpoints. Focuses on:
 *  - a token is required (security),
 *  - validation errors,
 *  - the ownership rule (you cannot see another user's task).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Register a user and return their JWT. */
    private String registerAndGetToken(String username) throws Exception {
        RegisterRequest request = new RegisterRequest(username, username + "@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void getTasks_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAndListTasks_shouldWorkForLoggedInUser() throws Exception {
        String token = registerAndGetToken("owner_" + UUID.randomUUID());

        TaskRequest request = new TaskRequest("My task", "desc",
                LocalDateTime.now().plusDays(3), Priority.HIGH, null, null);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My task"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("My task"));
    }

    @Test
    void createTask_withoutTitle_shouldReturnValidationError() throws Exception {
        String token = registerAndGetToken("invalid_" + UUID.randomUUID());

        // title is null -> @NotBlank should reject it.
        TaskRequest request = new TaskRequest(null, "desc",
                LocalDateTime.now().plusDays(1), Priority.LOW, null, null);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void getTask_belongingToAnotherUser_shouldReturnNotFound() throws Exception {
        String tokenA = registerAndGetToken("alice_" + UUID.randomUUID());
        String tokenB = registerAndGetToken("bob_" + UUID.randomUUID());

        // User A creates a task.
        TaskRequest request = new TaskRequest("Alice secret", "desc",
                LocalDateTime.now().plusDays(1), Priority.MEDIUM, null, null);
        MvcResult created = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long taskId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // User B tries to read Alice's task -> must be hidden (404).
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminEndpoint_asNormalUser_shouldReturnForbidden() throws Exception {
        String token = registerAndGetToken("normal_" + UUID.randomUUID());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
