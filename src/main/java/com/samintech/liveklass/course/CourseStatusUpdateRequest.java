package com.samintech.liveklass.course;

import jakarta.validation.constraints.NotNull;

public record CourseStatusUpdateRequest(
        @NotNull(message = "변경할 상태는 필수입니다") CourseStatus status
) {}
