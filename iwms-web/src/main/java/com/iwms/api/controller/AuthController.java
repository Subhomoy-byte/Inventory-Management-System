package com.iwms.api.controller;

import com.iwms.api.dto.LoginRequest;
import com.iwms.api.dto.LoginResponse;
import com.iwms.api.dto.UserDto;
import com.iwms.api.filter.CustomAuthFilter;
import com.iwms.auth.Authenticator;
import com.iwms.auth.SessionManager;
import com.iwms.model.Permission;
import com.iwms.model.User;
import com.iwms.repository.Repository;
import com.iwms.storage.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Repository<User, String> userRepository;
    private final Authenticator authenticator;
    private final SessionManager sessionManager;
    private final AuditLogger auditLogger;

    public AuthController(Repository<User, String> userRepository,
                          Authenticator authenticator,
                          SessionManager sessionManager,
                          AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.authenticator = authenticator;
        this.sessionManager = sessionManager;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) throws Exception {
        if (request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password required"));
        }

        Optional<User> userOpt = userRepository.findById(request.username().trim().toLowerCase());
        if (userOpt.isEmpty()) {
            auditLogger.log(request.username(), "LOGIN", "SYSTEM", "FAILURE", "Unknown user account");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        try {
            boolean valid = authenticator.authenticate(user, request.password());
            if (!valid) {
                auditLogger.log(user.getUsername(), "LOGIN", "SYSTEM", "FAILURE", "Incorrect password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
            }

            SessionManager.Session session = sessionManager.createSession(user);
            auditLogger.log(user.getUsername(), "LOGIN", "SYSTEM", "SUCCESS", "Logged in successfully");

            UserDto userDto = toDto(user);
            return ResponseEntity.ok(new LoginResponse(session.token(), userDto));
        } catch (Exception e) {
            auditLogger.log(user.getUsername(), "LOGIN", "SYSTEM", "LOCKED", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            sessionManager.invalidateSession(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toDto(user));
    }

    private UserDto toDto(User user) {
        List<String> perms = user.getPermissions().stream().map(Permission::name).toList();
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole().name(), perms);
    }
}
