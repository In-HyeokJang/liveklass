package com.samintech.liveklass.enrollment;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import com.samintech.liveklass.course.Course;
import com.samintech.liveklass.course.CourseRepository;
import com.samintech.liveklass.course.CourseStatus;
import com.samintech.liveklass.user.User;
import com.samintech.liveklass.user.UserRepository;
import com.samintech.liveklass.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollmentService {

    private static final int CANCEL_WINDOW_DAYS = 7;

    // 정원 계산 기준: WAITLISTED는 자리를 차지하지 않음
    private static final List<EnrollmentStatus> CAPACITY_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    // 중복 신청 방지 기준: 대기 중인 경우도 재신청 불가
    private static final List<EnrollmentStatus> NON_CANCELLED_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED);

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * 강의 수강을 신청합니다.
     * 정원이 남아 있으면 PENDING, 정원이 꽉 찼으면 WAITLISTED로 등록합니다.
     * 비관적 락으로 동시에 여러 명이 마지막 자리를 신청해도 정원을 정확히 관리합니다.
     */
    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long userId) {
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.OPEN) {
            throw new BusinessException(ErrorCode.COURSE_NOT_ENROLLABLE);
        }

        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, NON_CANCELLED_STATUSES)) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() != UserRole.CLASSMATE) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ROLE);
        }

        int activeCount = enrollmentRepository.countByCourseIdAndStatusIn(courseId, CAPACITY_STATUSES);
        EnrollmentStatus status = activeCount >= course.getCapacity()
                ? EnrollmentStatus.WAITLISTED
                : EnrollmentStatus.PENDING;

        Enrollment enrollment = Enrollment.builder()
                .course(course)
                .user(user)
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .build();

        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public EnrollmentResponse confirm(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLMENT_OWNER);
        }

        enrollment.confirm();
        return EnrollmentResponse.from(enrollment);
    }

    /**
     * 수강 신청을 취소합니다.
     * PENDING 또는 CONFIRMED 취소 시 자리가 생기므로 대기열 첫 번째 대기자를 PENDING으로 승격합니다.
     */
    @Transactional
    public EnrollmentResponse cancel(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLMENT_OWNER);
        }

        boolean capacityFreed = CAPACITY_STATUSES.contains(enrollment.getStatus());

        enrollment.cancel(CANCEL_WINDOW_DAYS);

        if (capacityFreed) {
            enrollmentRepository
                    .findFirstByCourseIdAndStatusOrderByEnrolledAtAsc(
                            enrollment.getCourse().getId(), EnrollmentStatus.WAITLISTED)
                    .ifPresent(Enrollment::promote);
        }

        return EnrollmentResponse.from(enrollment);
    }

    public Page<EnrollmentResponse> getMyEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findByUserId(userId, pageable)
                .map(EnrollmentResponse::from);
    }

    public List<EnrollmentResponse> getCourseEnrollments(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_CREATOR);
        }

        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }
}