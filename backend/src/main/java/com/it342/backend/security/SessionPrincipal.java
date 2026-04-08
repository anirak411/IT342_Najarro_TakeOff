package com.it342.backend.security;

import java.time.Instant;

public record SessionPrincipal(
        Long userId,
        Instant expiresAt
) {
}
