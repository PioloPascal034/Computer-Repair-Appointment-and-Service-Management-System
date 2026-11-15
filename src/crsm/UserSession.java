package crsm;

public record UserSession(String userId, String email, String fullName, String username, String role, String accessToken, String avatarPath) {
    public UserSession withAvatarPath(String path) { return new UserSession(userId, email, fullName, username, role, accessToken, path); }
    public UserSession withIdentity(String name, String handle) { return new UserSession(userId, email, name, handle, role, accessToken, avatarPath); }
}
