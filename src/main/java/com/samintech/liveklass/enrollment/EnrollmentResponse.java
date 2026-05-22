package com.samintech.liveklass.enrollment;

import java.time.LocalDateTime;

/**
 * 수강 신청 응답 DTO.
 *
 * 수강 신청·확정·취소 모든 응답에서 공통
 * 각 시점의 상태 값({@link EnrollmentStatus})으로 클라이언트가 현재 상태를 파악
 *
 * @param courseTitle  강의 제목 (course 엔티티에서 JOIN하여 가져옴)
 * @param username     수강생 이름 (user 엔티티에서 JOIN하여 가져옴)
 * @param status       현재 수강 신청 상태
 * @param enrolledAt   최초 신청 일시 (취소 후 재신청 시 갱신됨)
 * @param confirmedAt  결제 확정 일시 (CONFIRMED 상태가 아니면 null)
 * @param cancelledAt  취소 처리 일시 (CANCELLED 상태가 아니면 null)
 */
public record EnrollmentResponse(
        Long id,
        Long courseId,
        String courseTitle,
        Long userId,
        String username,
        EnrollmentStatus status,
        LocalDateTime enrolledAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getUser().getId(),
                enrollment.getUser().getUsername(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getConfirmedAt(),
                enrollment.getCancelledAt()
        );
    }
}
