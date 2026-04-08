package com.it342.backend.security;

import com.it342.backend.model.User;
import com.it342.backend.model.UserRole;
import com.it342.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final Map<String, SessionPrincipal> sessions = new ConcurrentHashMap<>();
    private final UserRepository userRepository;

    public SessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String createSession(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new SessionPrincipal(user.getId(), Instant.now().plus(SESSION_TTL)));
        return token;
    }

    public void revoke(String sessionToken) {
        String normalized = normalizeToken(sessionToken);
        if (!normalized.isBlank()) {
            sessions.remove(normalized);
        }
    }

    public Optional<User> resolveUser(String sessionToken) {
        String normalized = normalizeToken(sessionToken);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        SessionPrincipal principal = sessions.get(normalized);
        if (principal == null) {
            return Optional.empty();
        }

        if (principal.expiresAt().isBefore(Instant.now())) {
            sessions.remove(normalized);
            return Optional.empty();
        }

        Optional<User> user = userRepository.findById(principal.userId());
        if (user.isEmpty()) {
            sessions.remove(normalized);
            return Optional.empty();
        }

        return user;
    }

    public User requireUser(String sessionToken) {
        return resolveUser(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid session required"));
    }

    public User requireAdminUser(String sessionToken) {
        User user = requireUser(sessionToken);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
        return user;
    }

    private String normalizeToken(String token) {
        return token == null ? "" : token.trim();
    }
}
