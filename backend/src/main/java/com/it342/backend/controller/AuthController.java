package com.it342.backend.controller;

import com.it342.backend.dto.ApiResponse;
import com.it342.backend.dto.LoginRequest;
import com.it342.backend.dto.RegisterRequest;
import com.it342.backend.dto.UserProfileResponse;
import com.it342.backend.model.User;
import com.it342.backend.model.UserRole;
import com.it342.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword() == null ? "" : request.getPassword();
        String fullName = request.getFullName() == null ? "" : request.getFullName().trim();
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();

        if (normalizedEmail.isBlank() || rawPassword.isBlank() || fullName.isBlank() || displayName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "fullName, displayName, email, and password are required"));
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email already exists!"));
        }

        if (userRepository.existsByDisplayName(displayName)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Display name already taken!"));
        }

        UserRole requestedRole = parseRole(request.getRole());
        boolean hasAdmin = userRepository.existsByRole(UserRole.ADMIN);
        UserRole finalRole =
                (!hasAdmin && requestedRole == UserRole.ADMIN)
                        ? UserRole.ADMIN
                        : UserRole.USER;

        User user = new User(
                fullName,
                displayName,
                normalizedEmail,
                encoder.encode(rawPassword)
        );
        user.setRole(finalRole);

        userRepository.save(user);

        return ResponseEntity.ok(
                new ApiResponse(
                        true,
                        finalRole == UserRole.ADMIN
                                ? "User registered successfully as ADMIN."
                                : "User registered successfully!"
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword() == null ? "" : request.getPassword();

        if (normalizedEmail.isBlank() || rawPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email and password are required"));
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "User not found!"));
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();

        if (!passwordMatches(rawPassword, storedPassword)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Invalid password!"));
        }

        // Automatically migrate legacy plaintext passwords after successful login.
        if (!isBcryptHash(storedPassword)) {
            user.setPassword(encoder.encode(rawPassword));
            userRepository.save(user);
        }

        UserProfileResponse profile =
                new UserProfileResponse(
                        user.getDisplayName(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getProfilePicUrl(),
                        user.getCoverPicUrl(),
                        user.getRole().name()
                );

        return ResponseEntity.ok(
                new ApiResponse(true, "Login successful!", profile)
        );
    }

    @GetMapping("/admin-exists")
    public ResponseEntity<ApiResponse> adminExists() {
        boolean exists = userRepository.existsByRole(UserRole.ADMIN);
        return ResponseEntity.ok(
                new ApiResponse(true, "Admin status fetched.", exists)
        );
    }

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<ApiResponse> bootstrapAdmin(@RequestBody LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword() == null ? "" : request.getPassword();

        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse(false, "An admin account already exists."));
        }

        if (normalizedEmail.isBlank() || rawPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email and password are required."));
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "User not found."));
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();
        if (!passwordMatches(rawPassword, storedPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid credentials."));
        }

        if (!isBcryptHash(storedPassword)) {
            user.setPassword(encoder.encode(rawPassword));
        }

        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        UserProfileResponse profile =
                new UserProfileResponse(
                        user.getDisplayName(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getProfilePicUrl(),
                        user.getCoverPicUrl(),
                        user.getRole().name()
                );

        return ResponseEntity.ok(
                new ApiResponse(true, "Admin role granted.", profile)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        // Current session handling is client-side. This endpoint exists
        // to complete the API contract until JWT token revocation is added.
        return ResponseEntity.ok(
                new ApiResponse(true, "Logout successful.")
        );
    }

    private UserRole parseRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return UserRole.USER;
        }

        try {
            return UserRole.valueOf(rawRole.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UserRole.USER;
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || rawPassword.isBlank() || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (isBcryptHash(storedPassword)) {
            return encoder.matches(rawPassword, storedPassword);
        }

        return rawPassword.equals(storedPassword);
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
