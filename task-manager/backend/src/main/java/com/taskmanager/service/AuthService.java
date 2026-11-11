package com.taskmanager.service;

import com.taskmanager.dto.AuthResponse;
import com.taskmanager.dto.LoginRequest;
import com.taskmanager.dto.RegisterRequest;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ConflictException;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles registration and login. This is where passwords are hashed and where
 * JWTs are created.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /** Create a new USER account and return a token so the client is logged in. */
    public AuthResponse register(RegisterRequest request) {
        // Friendly, explicit errors instead of a raw database constraint error.
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        // Hash the password with BCrypt before saving. NEVER store plain text.
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER); // everyone starts as a normal USER

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getUsername(), saved.getRole().name());
        return toAuthResponse(token, saved);
    }

    /** Check credentials and return a token. */
    public AuthResponse login(LoginRequest request) {
        // This asks Spring Security to check the username + BCrypt password.
        // If they are wrong it throws BadCredentialsException, which our
        // GlobalExceptionHandler turns into a 401 JSON response.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        // Credentials were correct, so the user must exist.
        User user = userRepository.findByUsername(request.username()).orElseThrow();

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return toAuthResponse(token, user);
    }

    private AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
