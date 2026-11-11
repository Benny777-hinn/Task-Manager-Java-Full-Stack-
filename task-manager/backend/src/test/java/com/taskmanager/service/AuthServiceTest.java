package com.taskmanager.service;

import com.taskmanager.dto.AuthResponse;
import com.taskmanager.dto.LoginRequest;
import com.taskmanager.dto.RegisterRequest;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService with all collaborators mocked.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_whenUsernameTaken_shouldThrowConflict() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");

        assertThrows(ConflictException.class, () -> authService.register(request));
        // Nothing should be saved when the username is taken.
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldHashPasswordAndReturnToken() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(jwtService.generateToken("bob", "USER")).thenReturn("jwt-token");

        // save() returns the same user (with an id set, as a real save would).
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthResponse response = authService.register(
                new RegisterRequest("bob", "bob@example.com", "password123"));

        assertEquals("jwt-token", response.token());
        assertEquals(Role.USER, response.role());
        // The stored password must be the encoded value, never the raw one.
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void login_shouldAuthenticateAndReturnToken() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setRole(Role.USER);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("alice", "password123"));

        assertEquals("jwt-token", response.token());
        assertEquals("alice", response.username());
        // AuthenticationManager.authenticate() must have been called once.
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
