package com.it342.backend.dto;

public class AuthSessionResponse {

    private final String displayName;
    private final String fullName;
    private final String email;
    private final String profilePicUrl;
    private final String coverPicUrl;
    private final String role;
    private final String sessionToken;

    public AuthSessionResponse(
            String displayName,
            String fullName,
            String email,
            String profilePicUrl,
            String coverPicUrl,
            String role,
            String sessionToken
    ) {
        this.displayName = displayName;
        this.fullName = fullName;
        this.email = email;
        this.profilePicUrl = profilePicUrl;
        this.coverPicUrl = coverPicUrl;
        this.role = role;
        this.sessionToken = sessionToken;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public String getCoverPicUrl() {
        return coverPicUrl;
    }

    public String getRole() {
        return role;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
