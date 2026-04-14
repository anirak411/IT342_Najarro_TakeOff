package com.it342.backend.controller;

import com.it342.backend.dto.ApiResponse;
import com.it342.backend.dto.AuthSessionResponse;
import com.it342.backend.dto.LoginRequest;
import com.it342.backend.dto.RegisterRequest;
import com.it342.backend.model.User;
import com.it342.backend.model.UserRole;
import com.it342.backend.repository.UserRepository;
import com.it342.backend.security.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Locale;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{8,}$");

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword() == null ? "" : request.getPassword();
        String fullName = request.getFullName() == null ? "" : request.getFullName().trim();
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();

        if (normalizedEmail.isBlank() || rawPassword.isBlank() || fullName.isBlank() || displayName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Full name, username, email, and password are required"));
        }

        if (!isValidUsername(displayName)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Username must be at least 8 characters and can only contain letters, numbers, and underscore."));
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email already exists!"));
        }

        if (userRepository.existsByDisplayNameIgnoreCase(displayName)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Username already taken!"));
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

        String sessionToken = sessionService.createSession(user);
        AuthSessionResponse profile = buildAuthSessionResponse(user, sessionToken);

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

        String sessionToken = sessionService.createSession(user);
        AuthSessionResponse profile = buildAuthSessionResponse(user, sessionToken);

        return ResponseEntity.ok(
                new ApiResponse(true, "Admin role granted.", profile)
        );
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse> getSession(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        User user = sessionService.requireUser(sessionToken);
        return ResponseEntity.ok(
                new ApiResponse(
                        true,
                        "Session active.",
                        buildAuthSessionResponse(user, sessionToken == null ? "" : sessionToken.trim())
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        sessionService.revoke(sessionToken);
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

    private boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    private AuthSessionResponse buildAuthSessionResponse(User user, String sessionToken) {
        return new AuthSessionResponse(
                user.getDisplayName(),
                user.getFullName(),
                user.getEmail(),
                user.getProfilePicUrl(),
                user.getCoverPicUrl(),
                user.getRole().name(),
                sessionToken
        );
    }
}
