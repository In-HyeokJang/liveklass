package com.samintech.liveklass.enrollment;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import com.samintech.liveklass.course.Course;
import com.samintech.liveklass.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 수강 신청(Enrollment) 도메인 엔티티
 * 클래스메이트(수강생)가 특정 강의에 수강 신청한 내역을 관리하며,
 * 신청 상태(PENDING, CONFIRMED, CANCELLED) 및 관련 비즈니스 규칙(취소 기한 등)을 캡슐화
 */
@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;

    // 취소 후 재신청 시 기존 row를 재활성화 (DB 유니크 제약 우회)
    public void reactivate(EnrollmentStatus newStatus) {
        if (this.status != EnrollmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = newStatus;
        this.enrolledAt = LocalDateTime.now();
        this.cancelledAt = null;
        this.confirmedAt = null;
    }

    public void promote() {
        if (this.status != EnrollmentStatus.WAITLISTED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = EnrollmentStatus.PENDING;
    }

    /**
     * 수강 신청 상태를 '결제 완료(CONFIRMED)'로 변경
     * 비즈니스 규칙: 수강 신청은 PENDING(결제 대기) 상태에서만 CONFIRMED로 변경할 수 있다.
     */
    public void confirm() {
        if (this.status == EnrollmentStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_CONFIRMED);
        }
        if (this.status == EnrollmentStatus.WAITLISTED) {
            throw new BusinessException(ErrorCode.ENROLLMENT_WAITLISTED_NOT_CONFIRMABLE);
        }
        if (this.status == EnrollmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ENROLLMENT_CANCELLED_NOT_CONFIRMABLE);
        }
        if (this.status != EnrollmentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ENROLLMENT_NOT_CONFIRMABLE);
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    /**
     * 수강 신청 취소
     * 비즈니스 규칙:
     * - 이미 취소된 상태(CANCELLED)라면 중복 취소가 불가능
     * - 결제 확정(CONFIRMED) 상태인 경우, 설정된 취소 가능 기한(cancelWindowDays) 이내에만 취소 가능
     * - PENDING 상태는 기한 제약 없이 언제든 취소 가능
     *
     * @param cancelWindowDays 취소 가능한 최대 기한 (일 수)
     */
    public void cancel(int cancelWindowDays) {
        if (this.status == EnrollmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ENROLLMENT_NOT_CANCELLABLE);
        }

        if (this.status == EnrollmentStatus.CONFIRMED) {
            if (this.confirmedAt.plusDays(cancelWindowDays).isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.CANCEL_PERIOD_EXCEEDED);
            }
        }

        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
}