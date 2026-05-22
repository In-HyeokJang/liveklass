package com.samintech.liveklass.course;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import com.samintech.liveklass.enrollment.EnrollmentRepository;
import com.samintech.liveklass.enrollment.EnrollmentStatus;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock UserRepository userRepository;
    @InjectMocks CourseService courseService;

    private User creator;
    private User otherUser;
    private Course openCourse;

    @BeforeEach
    void setUp() {
        creator = User.builder().id(1L).username("creator").role(UserRole.CREATOR).build();
        otherUser = User.builder().id(2L).username("other").role(UserRole.CLASSMATE).build();

        openCourse = Course.builder()
                .id(10L)
                .creator(creator)
                .title("Java 기초")
                .description("설명")
                .price(50000)
                .capacity(30)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .status(CourseStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("강의 생성 시 DRAFT 상태로 저장된다")
    void createCourse_shouldCreateAsDraft() {
        CourseCreateRequest request = new CourseCreateRequest(
                "신규 강의", "설명", 30000, 20,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(35));

        given(userRepository.findById(1L)).willReturn(Optional.of(creator));
        given(courseRepository.save(any(Course.class))).willAnswer(inv -> {
            Course c = inv.getArgument(0);
            return Course.builder()
                    .id(99L).creator(c.getCreator()).title(c.getTitle())
                    .description(c.getDescription()).price(c.getPrice())
                    .capacity(c.getCapacity()).startDate(c.getStartDate())
                    .endDate(c.getEndDate()).status(c.getStatus()).build();
        });

        CourseResponse result = courseService.createCourse(1L, request);

        assertThat(result.status()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.title()).isEqualTo("신규 강의");
        assertThat(result.enrolledCount()).isZero();
    }

    @Test
    @DisplayName("CLASSMATE가 강의 생성 시도 시 예외 발생")
    void createCourse_shouldThrowWhenUserIsNotCreator() {
        given(userRepository.findById(2L)).willReturn(Optional.of(otherUser)); // CLASSMATE

        CourseCreateRequest request = new CourseCreateRequest(
                "강의", "설명", 0, 10,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> courseService.createCourse(2L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_ROLE.getMessage());
    }

    @Test
    @DisplayName("시작일이 종료일 이후인 경우 예외 발생")
    void createCourse_shouldThrowWhenStartDateAfterEndDate() {
        CourseCreateRequest request = new CourseCreateRequest(
                "강의", "설명", 0, 10,
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(1)); // 역전

        assertThatThrownBy(() -> courseService.createCourse(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_DATE_RANGE.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 강의 생성 시 예외 발생")
    void createCourse_shouldThrowWhenUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        CourseCreateRequest request = new CourseCreateRequest(
                "강의", "설명", 0, 10,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> courseService.createCourse(999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("DRAFT → OPEN 상태 변경 성공")
    void updateStatus_draftToOpen_shouldSucceed() {
        Course draftCourse = Course.builder()
                .id(10L).creator(creator).title("강의").description("설명")
                .price(0).capacity(10)
                .startDate(LocalDate.now().plusDays(1)).endDate(LocalDate.now().plusDays(30))
                .status(CourseStatus.DRAFT).build();

        given(courseRepository.findByIdWithCreator(10L)).willReturn(Optional.of(draftCourse));
        given(enrollmentRepository.countByCourseIdAndStatusIn(any(), any())).willReturn(0);

        CourseResponse result = courseService.updateStatus(10L, 1L, CourseStatus.OPEN);

        assertThat(result.status()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 상태 변경 시도 시 예외 발생")
    void updateStatus_shouldThrowWhenNotCreator() {
        given(courseRepository.findByIdWithCreator(10L)).willReturn(Optional.of(openCourse));

        assertThatThrownBy(() -> courseService.updateStatus(10L, 2L, CourseStatus.CLOSED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_COURSE_CREATOR.getMessage());
    }

    @Test
    @DisplayName("CLOSED → OPEN 등 잘못된 상태 전이 시 예외 발생")
    void updateStatus_shouldThrowOnInvalidTransition() {
        Course closedCourse = Course.builder()
                .id(10L).creator(creator).title("강의").description("설명")
                .price(0).capacity(10)
                .startDate(LocalDate.now().plusDays(1)).endDate(LocalDate.now().plusDays(30))
                .status(CourseStatus.CLOSED).build();

        given(courseRepository.findByIdWithCreator(10L)).willReturn(Optional.of(closedCourse));

        assertThatThrownBy(() -> courseService.updateStatus(10L, 1L, CourseStatus.OPEN))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_STATUS_TRANSITION.getMessage());
    }

    @Test
    @DisplayName("강의 목록 조회 시 신청 인원이 포함된다")
    void getCourses_shouldIncludeEnrollmentCount() {
        given(courseRepository.searchCourses(CourseStatus.OPEN, null, null, null, null, null)).willReturn(List.of(openCourse));
        Object[] row = {10L, 5L};
        given(enrollmentRepository.countByCourseIdsAndStatusIn(any(), any()))
                .willReturn(java.util.Collections.singletonList(row));

        List<CourseResponse> result = courseService.getCourses(CourseStatus.OPEN, null, null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).enrolledCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("고급 필터 조건에 맞게 강의 목록을 조회하고 남은 정원 필터를 처리한다")
    void getCourses_advancedFilters_shouldWork() {
        Course freeCourse = Course.builder()
                .id(11L).creator(creator).title("무료 Java 기초").description("설명")
                .price(0).capacity(5)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(30))
                .status(CourseStatus.OPEN).build();

        given(courseRepository.searchCourses(CourseStatus.OPEN, "Java", 0, 10000, null, null))
                .willReturn(List.of(freeCourse));

        Object[] row = {11L, 5L};
        given(enrollmentRepository.countByCourseIdsAndStatusIn(any(), any()))
                .willReturn(java.util.Collections.singletonList(row));

        List<CourseResponse> result = courseService.getCourses(
                CourseStatus.OPEN, "Java", 0, 10000, null, null, true);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("강의 상세 조회 - 존재하지 않는 강의 예외")
    void getCourse_shouldThrowWhenNotFound() {
        given(courseRepository.findByIdWithCreator(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("크리에이터 전용 수강생 목록 - 다른 사용자 접근 시 예외")
    void getCourseEnrollments_shouldThrowWhenNotCreator() {
        given(courseRepository.findByIdWithCreator(10L)).willReturn(Optional.of(openCourse));

        assertThatThrownBy(() -> courseService.updateStatus(10L, otherUser.getId(), CourseStatus.CLOSED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_COURSE_CREATOR.getMessage());
    }
}
