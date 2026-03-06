package com.it342.backend.controller;

import com.it342.backend.dto.UpdateUserRoleRequest;
import com.it342.backend.dto.UserSummaryResponse;
import com.it342.backend.model.User;
import com.it342.backend.model.UserRole;
import com.it342.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserSummaryResponse> getAllUsers() {
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

    @PutMapping("/{id}/role")
    public UserSummaryResponse updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request
    ) {
        String adminEmail = normalizeEmail(request.getAdminEmail());
        if (adminEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "adminEmail is required");
        }

        User adminUser = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        if (adminUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

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

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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
}
