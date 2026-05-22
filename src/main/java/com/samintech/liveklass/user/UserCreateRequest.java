package com.samintech.liveklass.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
    @NotBlank(message = "사용자 이름은 필수입니다")
    String username,

    @NotNull(message = "역할은 필수입니다")
    UserRole role
) {}
