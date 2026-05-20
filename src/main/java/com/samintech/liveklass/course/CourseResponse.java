package com.samintech.liveklass.course;

import java.time.LocalDate;

public record CourseResponse(
        Long id,
        String title,
        String description,
        int price,
        int capacity,
        int enrolledCount,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status
) {
    public static CourseResponse from(Course course, int enrolledCount) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice(),
                course.getCapacity(),
                enrolledCount,
                course.getStartDate(),
                course.getEndDate(),
                course.getStatus()
        );
    }
}
