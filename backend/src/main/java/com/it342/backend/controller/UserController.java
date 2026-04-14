package com.it342.backend.controller;

import com.it342.backend.dto.UpdateUserRoleRequest;
import com.it342.backend.dto.UpdateUserProfileRequest;
import com.it342.backend.dto.UserSummaryResponse;
import com.it342.backend.model.User;
import com.it342.backend.model.UserRole;
import com.it342.backend.repository.UserRepository;
import com.it342.backend.security.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{8,}$");

    private final UserRepository userRepository;
    private final SessionService sessionService;

    public UserController(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/admin")
    public List<UserSummaryResponse> getAllUsersForAdmin(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        sessionService.requireAdminUser(sessionToken);
        return userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/me")
    public UserSummaryResponse getUserByEmail(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toSummary(user);
    }

    @PutMapping("/media")
    public UserSummaryResponse updateUserMedia(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "").trim();
        if (email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        if (payload.containsKey("profilePicUrl")) {
            user.setProfilePicUrl(payload.get("profilePicUrl"));
        }

        if (payload.containsKey("coverPicUrl")) {
            user.setCoverPicUrl(payload.get("coverPicUrl"));
        }

        User saved = userRepository.save(user);
        return toSummary(saved);
    }

    @PutMapping("/profile")
    public UserSummaryResponse updateUserProfile(@RequestBody UpdateUserProfileRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();
        String fullName = request.getFullName() == null ? "" : request.getFullName().trim();

        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (displayName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        if (!isValidUsername(displayName)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username must be at least 8 characters and can only contain letters, numbers, and underscore"
            );
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentDisplayName = user.getDisplayName() == null ? "" : user.getDisplayName();
        if (!currentDisplayName.equalsIgnoreCase(displayName)
                && userRepository.existsByDisplayNameIgnoreCase(displayName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
        }

        user.setDisplayName(displayName);
        if (!fullName.isBlank()) {
            user.setFullName(fullName);
        }

        User saved = userRepository.save(user);
        return toSummary(saved);
    }

    @PutMapping("/{id}/role")
    public UserSummaryResponse updateUserRole(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody UpdateUserRoleRequest request
    ) {
        sessionService.requireAdminUser(sessionToken);

        UserRole nextRole = parseRole(request.getRole());

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        if (target.getRole() == UserRole.ADMIN
                && nextRole == UserRole.USER
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one admin account must remain"
            );
        }

        target.setRole(nextRole);
        User saved = userRepository.save(target);
        return toSummary(saved);
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getDisplayName(),
                user.getFullName(),
                user.getEmail(),
                user.getProfilePicUrl(),
                user.getCoverPicUrl(),
                user.getRole().name()
        );
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return UserRole.USER;
        }

        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
    }

    private boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.trim()).matches();
    }
}
