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
import java.util.Optional;

/**
 * 수강 신청(Enrollment) 도메인의 비즈니스 로직을 처리하는 서비스.
 * 동시성 제어 전략:
 *   수강 신청({@code enroll}): 강의 row에 비관적 락({@code SELECT FOR UPDATE})을 걸어
 *       여러 사용자가 동시에 마지막 자리를 신청해도 정원 초과를 방지
 *   수강 취소({@code cancel}): 대기열 승격 시에도 강의 row에 비관적 락을 걸어
 *       동시 취소 시 동일 대기자가 중복 승격되는 것을 방지
 */
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
     * 강의 수강을 신청
     * 정원이 남아 있으면 PENDING, 정원이 꽉 찼으면 WAITLISTED로 등록
     * 비관적 락으로 동시에 여러 명이 마지막 자리를 신청해도 정원을 정확히 관리
     */
    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long userId) {
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.OPEN) {
            throw new BusinessException(ErrorCode.COURSE_NOT_ENROLLABLE);
        }

        if (course.isExpired()) {
            throw new BusinessException(ErrorCode.COURSE_EXPIRED);
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

        // DB 유니크 제약(user_id, course_id) 위반 방지
        // 이전에 취소한 내역이 있으면 새 row INSERT 대신 해당 row를 재활성화
        Optional<Enrollment> existing = enrollmentRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, EnrollmentStatus.CANCELLED);
        if (existing.isPresent()) {
            existing.get().reactivate(status);
            return EnrollmentResponse.from(existing.get());
        }

        Enrollment enrollment = Enrollment.builder()
                .course(course)
                .user(user)
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .build();

        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    /**
     * 수강 신청을 결제 확정(CONFIRMED) 처리
     * PENDING 상태인 본인의 신청만 확정할 수 있다.
     *
     * @param enrollmentId 확정할 수강 신청 ID
     * @param userId       요청자 ID
     */
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
     * 수강 신청을 취소
     * PENDING 또는 CONFIRMED 취소 시 자리가 생기므로 대기열 첫 번째 대기자를 PENDING으로 승격
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
            // 동시 취소 시 동일 대기자가 중복 승격되는 것을 막기 위해 강의에 비관적 락 획득
            courseRepository.findByIdWithLock(enrollment.getCourse().getId());
            enrollmentRepository
                    .findFirstByCourseIdAndStatusOrderByEnrolledAtAsc(
                            enrollment.getCourse().getId(), EnrollmentStatus.WAITLISTED)
                    .ifPresent(Enrollment::promote);
        }

        return EnrollmentResponse.from(enrollment);
    }

    /**
     * 본인의 수강 신청 내역을 페이지네이션으로 조회
     * 기본 정렬: 신청일시({@code enrolledAt}) 내림차순, 페이지 크기 10.
     */
    public Page<EnrollmentResponse> getMyEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findByUserId(userId, pageable)
                .map(EnrollmentResponse::from);
    }

    /**
     * 특정 강의의 수강생 목록을 조회, 해당 강의의 개설자만 호출
     *
     * @param courseId 조회할 강의 ID
     * @param userId   요청자 ID (개설자 검증에 사용)
     */
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