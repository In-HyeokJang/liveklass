package com.samintech.liveklass.enrollment;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import com.samintech.liveklass.course.Course;
import com.samintech.liveklass.course.CourseRepository;
import com.samintech.liveklass.course.CourseStatus;
import com.samintech.liveklass.user.User;
import com.samintech.liveklass.user.UserRepository;
import com.samintech.liveklass.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock EnrollmentRepository enrollmentRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserRepository userRepository;
    @InjectMocks EnrollmentService enrollmentService;

    private User student;
    private Course openCourse;

    @BeforeEach
    void setUp() {
        User creator = User.builder().id(1L).username("creator").role(UserRole.CREATOR).build();
        student = User.builder().id(2L).username("student").role(UserRole.CLASSMATE).build();

        openCourse = Course.builder()
                .id(10L).creator(creator).title("Java 강의").description("설명")
                .price(50000).capacity(2)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .status(CourseStatus.OPEN).build();
    }

    @Test
    @DisplayName("수강 신청 성공 - PENDING 상태로 생성")
    void enroll_shouldCreatePendingEnrollment() {
        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.countByCourseIdAndStatusIn(any(), any())).willReturn(0);
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(userRepository.findById(2L)).willReturn(Optional.of(student));
        given(enrollmentRepository.save(any())).willAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            return Enrollment.builder()
                    .id(1L).user(e.getUser()).course(e.getCourse())
                    .status(e.getStatus()).enrolledAt(e.getEnrolledAt()).build();
        });

        EnrollmentResponse result = enrollmentService.enroll(10L, 2L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.courseTitle()).isEqualTo("Java 강의");
    }

    @Test
    @DisplayName("OPEN 아닌 강의 신청 시 예외 발생")
    void enroll_shouldThrowWhenCourseNotOpen() {
        Course draftCourse = Course.builder()
                .id(10L).creator(null).title("강의").description("설명")
                .price(0).capacity(10)
                .startDate(LocalDate.now().plusDays(1)).endDate(LocalDate.now().plusDays(30))
                .status(CourseStatus.DRAFT).build();

        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> enrollmentService.enroll(10L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COURSE_NOT_ENROLLABLE.getMessage());
    }

    @Test
    @DisplayName("CLOSED 상태 강의 신청 시 예외 발생")
    void enroll_shouldThrowWhenCourseClosed() {
        Course closedCourse = Course.builder()
                .id(10L).creator(null).title("강의").description("설명")
                .price(0).capacity(10)
                .startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().minusDays(1))
                .status(CourseStatus.CLOSED).build();

        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(closedCourse));

        assertThatThrownBy(() -> enrollmentService.enroll(10L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COURSE_NOT_ENROLLABLE.getMessage());
    }

    @Test
    @DisplayName("수강 기간이 지난 강의(OPEN 상태이지만 endDate 경과) 신청 시 예외 발생")
    void enroll_shouldThrowWhenCourseExpired() {
        Course expiredCourse = Course.builder()
                .id(10L).creator(null).title("강의").description("설명")
                .price(0).capacity(10)
                .startDate(LocalDate.now().minusDays(30)).endDate(LocalDate.now().minusDays(1))
                .status(CourseStatus.OPEN).build(); // OPEN이지만 endDate 경과

        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(expiredCourse));

        assertThatThrownBy(() -> enrollmentService.enroll(10L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COURSE_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("정원 초과 시 대기열(WAITLISTED)로 등록")
    void enroll_shouldAddToWaitlistWhenFull() {
        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(userRepository.findById(2L)).willReturn(Optional.of(student));
        given(enrollmentRepository.countByCourseIdAndStatusIn(any(), any())).willReturn(2); // capacity=2, 만석
        given(enrollmentRepository.save(any())).willAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            return Enrollment.builder().id(2L).user(e.getUser()).course(e.getCourse())
                    .status(e.getStatus()).enrolledAt(e.getEnrolledAt()).build();
        });

        EnrollmentResponse result = enrollmentService.enroll(10L, 2L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.WAITLISTED);
    }

    @Test
    @DisplayName("취소 후 재신청 시 기존 CANCELLED row를 재활성화")
    void enroll_shouldReactivateCancelledEnrollment() {
        Enrollment cancelled = Enrollment.builder()
                .id(99L).user(student).course(openCourse)
                .status(EnrollmentStatus.CANCELLED)
                .enrolledAt(LocalDateTime.now().minusDays(5))
                .cancelledAt(LocalDateTime.now().minusDays(1))
                .build();

        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(userRepository.findById(2L)).willReturn(Optional.of(student));
        given(enrollmentRepository.countByCourseIdAndStatusIn(any(), any())).willReturn(0);
        given(enrollmentRepository.findByCourseIdAndUserIdAndStatus(10L, 2L, EnrollmentStatus.CANCELLED))
                .willReturn(Optional.of(cancelled));

        EnrollmentResponse result = enrollmentService.enroll(10L, 2L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.PENDING);
        verify(enrollmentRepository, never()).save(any()); // 새 row INSERT 없음
    }

    @Test
    @DisplayName("이미 신청한 강의 재신청 시 예외 발생")
    void enroll_shouldThrowWhenAlreadyEnrolled() {
        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(10L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ALREADY_ENROLLED.getMessage());
    }

    @Test
    @DisplayName("CREATOR가 수강 신청 시도 시 예외 발생")
    void enroll_shouldThrowWhenUserIsCreator() {
        User creator = User.builder().id(1L).username("creator").role(UserRole.CREATOR).build();
        given(courseRepository.findByIdWithLock(10L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(creator));

        assertThatThrownBy(() -> enrollmentService.enroll(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_ROLE.getMessage());
    }

    @Test
    @DisplayName("결제 확정 - PENDING → CONFIRMED 상태 전이")
    void confirm_shouldTransitionToConfirmed() {
        Enrollment pending = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(pending));

        EnrollmentResponse result = enrollmentService.confirm(1L, 2L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(result.confirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 확정된 수강 신청 재확정 시 예외 발생")
    void confirm_shouldThrowWhenAlreadyConfirmed() {
        Enrollment confirmed = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.CONFIRMED)
                .enrolledAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> enrollmentService.confirm(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ENROLLMENT_ALREADY_CONFIRMED.getMessage());
    }

    @Test
    @DisplayName("대기 중인 수강 신청 결제 확정 시 예외 발생")
    void confirm_shouldThrowWhenWaitlisted() {
        Enrollment waitlisted = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.WAITLISTED)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(waitlisted));

        assertThatThrownBy(() -> enrollmentService.confirm(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ENROLLMENT_WAITLISTED_NOT_CONFIRMABLE.getMessage());
    }

    @Test
    @DisplayName("취소된 수강 신청 결제 확정 시도 시 예외 발생")
    void confirm_shouldThrowWhenCancelled() {
        Enrollment cancelled = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.CANCELLED)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> enrollmentService.confirm(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ENROLLMENT_CANCELLED_NOT_CONFIRMABLE.getMessage());
    }

    @Test
    @DisplayName("타인의 수강 신청 결제 확정 시도 시 예외 발생")
    void confirm_shouldThrowWhenNotOwner() {
        Enrollment pending = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> enrollmentService.confirm(1L, 999L)) // 다른 userId
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_ENROLLMENT_OWNER.getMessage());
    }

    @Test
    @DisplayName("PENDING 상태 수강 취소 - 기간 제한 없이 취소 가능")
    void cancel_shouldCancelPendingAnytime() {
        Enrollment pending = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(pending));

        EnrollmentResponse result = enrollmentService.cancel(1L, 2L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("결제 후 7일 초과 시 취소 불가")
    void cancel_shouldThrowWhenCancelWindowExceeded() {
        Enrollment confirmed = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.CONFIRMED)
                .enrolledAt(LocalDateTime.now().minusDays(10))
                .confirmedAt(LocalDateTime.now().minusDays(8)) // 8일 전 결제
                .build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> enrollmentService.cancel(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CANCEL_PERIOD_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("결제 후 7일 이내 취소 가능")
    void cancel_shouldCancelWithinWindow() {
        Enrollment confirmed = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.CONFIRMED)
                .enrolledAt(LocalDateTime.now().minusDays(3))
                .confirmedAt(LocalDateTime.now().minusDays(3)) // 3일 전 결제
                .build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(confirmed));

        EnrollmentResponse result = enrollmentService.cancel(1L, 2L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("PENDING 취소 시 대기열 첫 번째 대기자가 PENDING으로 승격")
    void cancel_shouldPromoteWaitlistedWhenPendingCancelled() {
        Enrollment pending = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now()).build();

        User anotherStudent = User.builder().id(4L).username("student2").role(UserRole.CLASSMATE).build();
        Enrollment waitlisted = Enrollment.builder()
                .id(2L).user(anotherStudent).course(openCourse)
                .status(EnrollmentStatus.WAITLISTED)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(pending));
        given(enrollmentRepository.findFirstByCourseIdAndStatusOrderByEnrolledAtAsc(
                openCourse.getId(), EnrollmentStatus.WAITLISTED))
                .willReturn(Optional.of(waitlisted));

        enrollmentService.cancel(1L, 2L);

        assertThat(waitlisted.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    @DisplayName("WAITLISTED 취소 시 다른 대기자 승격 없음")
    void cancel_shouldNotPromoteWhenWaitlistedCancelled() {
        Enrollment waitlisted = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.WAITLISTED)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(waitlisted));

        EnrollmentResponse result = enrollmentService.cancel(1L, 2L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.CANCELLED);
        verify(enrollmentRepository, never())
                .findFirstByCourseIdAndStatusOrderByEnrolledAtAsc(any(), any());
    }

    @Test
    @DisplayName("PENDING 취소 시 강의에 비관적 락을 획득하여 대기열 승격 보호")
    void cancel_shouldAcquireLockOnCourseWhenCapacityFreed() {
        Enrollment pending = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(pending));

        enrollmentService.cancel(1L, 2L);

        verify(courseRepository).findByIdWithLock(openCourse.getId());
    }

    @Test
    @DisplayName("타인의 수강 신청 취소 시도 시 예외 발생")
    void cancel_shouldThrowWhenNotOwner() {
        Enrollment pending = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now()).build();

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> enrollmentService.cancel(1L, 999L)) // 다른 userId
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_ENROLLMENT_OWNER.getMessage());
    }

    @Test
    @DisplayName("강의별 수강생 목록 조회 - 개설자가 아닌 사용자가 조회 시도 시 예외 발생")
    void getCourseEnrollments_shouldThrowWhenNotCreator() {
        given(courseRepository.findById(10L)).willReturn(Optional.of(openCourse));

        assertThatThrownBy(() -> enrollmentService.getCourseEnrollments(10L, 999L)) // 개설자는 1L임
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_COURSE_CREATOR.getMessage());
    }

    @Test
    @DisplayName("강의별 수강생 목록 조회 - 개설자가 조회 시 성공")
    void getCourseEnrollments_shouldReturnListWhenCreator() {
        Enrollment enrollment = Enrollment.builder()
                .id(1L).user(student).course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now()).build();

        given(courseRepository.findById(10L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.findByCourseId(10L)).willReturn(List.of(enrollment));

        List<EnrollmentResponse> result = enrollmentService.getCourseEnrollments(10L, 1L); // 개설자 1L

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(student.getId());
    }
}

