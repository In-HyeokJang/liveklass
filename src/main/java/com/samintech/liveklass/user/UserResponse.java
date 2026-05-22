package com.samintech.liveklass.user;

public record UserResponse(
    Long id,
    String username,
    UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
