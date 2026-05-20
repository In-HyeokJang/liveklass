package com.samintech.liveklass.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CourseCreateRequest(
        @NotBlank(message = "제목은 필수입니다") String title,
        String description,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다") int price,
        @Min(value = 1, message = "정원은 1명 이상이어야 합니다") int capacity,
        @NotNull(message = "시작일은 필수입니다") LocalDate startDate,
        @NotNull(message = "종료일은 필수입니다") LocalDate endDate
) {}
