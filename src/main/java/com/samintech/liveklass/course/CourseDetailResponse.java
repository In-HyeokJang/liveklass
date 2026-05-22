package com.samintech.liveklass.course;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 강의 상세 조회 응답 DTO.
 * 단건 조회({@code GET /api/courses/{id}})에서 사용하며,
 * {@link CourseResponse}보다 더 많은 정보(크리에이터 정보, 생성 시각 등)를 포함
 *
 * @param creatorId   강의를 개설한 크리에이터의 사용자 ID
 * @param creatorName 크리에이터의 사용자명
 * @param createdAt   강의가 최초 생성된 일시
 * @param enrolledCount 현재 수강 확정/결제 대기 중인 수강생 수 (PENDING + CONFIRMED)
 */
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
