package com.samintech.liveklass.course;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CourseDetailResponse(
        Long id,
        Long creatorId,
        String creatorName,
        String title,
        String description,
        int price,
        int capacity,
        int enrolledCount,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status,
        LocalDateTime createdAt
) {
    public static CourseDetailResponse from(Course course, int enrolledCount) {
        return new CourseDetailResponse(
                course.getId(),
                course.getCreator().getId(),
                course.getCreator().getUsername(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice(),
                course.getCapacity(),
                enrolledCount,
                course.getStartDate(),
                course.getEndDate(),
                course.getStatus(),
                course.getCreatedAt()
        );
    }
}
