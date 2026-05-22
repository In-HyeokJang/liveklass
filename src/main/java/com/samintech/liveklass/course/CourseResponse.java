package com.samintech.liveklass.course;

import java.time.LocalDate;

/**
 * 강의 목록/요약 조회 응답 DTO.
 *
 * 강의 목록({@code GET /api/courses}) 및 상태 변경 응답에서 사용
 * 크리에이터 정보는 포함하지 않는 간략한 뷰, 상세 정보가 필요하면 {@link CourseDetailResponse}를 사용
 *
 * {@code from()} 정적 팩토리 메서드를 통해 {@link Course} 엔티티로부터 변환
 * 이렇게 하면 Controller/Service가 엔티티를 직접 반환하지 않아도 됨
 * (엔티티 내부 구조가 API 응답 포맷에 노출되는 것을 방지).
 *
 * @param enrolledCount 현재 수강 확정/결제 대기 중인 수강생 수 (PENDING + CONFIRMED)
 */
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
