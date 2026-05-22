package com.samintech.liveklass.course;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import com.samintech.liveklass.enrollment.EnrollmentRepository;
import com.samintech.liveklass.enrollment.EnrollmentStatus;
import com.samintech.liveklass.user.User;
import com.samintech.liveklass.user.UserRepository;
import com.samintech.liveklass.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 강의(Course) 도메인의 비즈니스 로직을 처리하는 서비스.
 *
 * 클래스 수준의 {@code @Transactional(readOnly = true)}는 모든 메서드를 읽기 전용 트랜잭션으로 기본 설정
 * DB 변경이 필요한 메서드({@code createCourse}, {@code updateStatus})에만 별도로 {@code @Transactional}을 선언
 * 이렇게 하면 실수로 읽기 전용 메서드에서 변경이 발생하는 것을 방지하고 DB 성능도 향상
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * 새 강의를 생성 초기 상태는 항상 {@code DRAFT}
     *
     * @param creatorId 강의를 만드는 사용자 ID ({@code X-User-Id} 헤더에서 전달됨)
     * @param request   강의 생성에 필요한 정보
     * @throws com.samintech.liveklass.common.BusinessException 요청자가 CREATOR가 아니거나 날짜가 역전된 경우
     */
    @Transactional
    public CourseResponse createCourse(Long creatorId, CourseCreateRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (creator.getRole() != UserRole.CREATOR) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ROLE);
        }

        Course course = Course.builder()
                .creator(creator)
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .capacity(request.capacity())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(CourseStatus.DRAFT)
                .build();

        return CourseResponse.from(courseRepository.save(course), 0);
    }

    /**
     * 고급 필터(상태, 제목 키워드, 가격 범위, 날짜 범위, 잔여 정원 여부)를 적용하여 강의 목록 조회
     * N+1 방지를 위해 수강생 수를 개별 쿼리가 아닌 GROUP BY 배치 쿼리로 한 번에 집계
     */
    public List<CourseResponse> getCourses(
            CourseStatus status,
            String title,
            Integer minPrice,
            Integer maxPrice,
            LocalDate startDate,
            LocalDate endDate,
            Boolean hasVacancies) {

        List<Course> courses = courseRepository.searchCourses(status, title, minPrice, maxPrice, startDate, endDate);

        if (courses.isEmpty()) return List.of();

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, Integer> countMap = enrollmentRepository
                .countByCourseIdsAndStatusIn(courseIds, ACTIVE_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        // 남은 자리가 있는 강의만 필터링 (capacity > activeCount)
        if (Boolean.TRUE.equals(hasVacancies)) {
            courses = courses.stream()
                    .filter(c -> c.getCapacity() > countMap.getOrDefault(c.getId(), 0))
                    .collect(Collectors.toList());
        }

        return courses.stream()
                .map(c -> CourseResponse.from(c, countMap.getOrDefault(c.getId(), 0)))
                .toList();
    }

    /** 강의 단건 상세 조회 (크리에이터 정보, 생성 시각 포함) */
    public CourseDetailResponse getCourse(Long courseId) {
        Course course = courseRepository.findByIdWithCreator(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        int count = enrollmentRepository.countByCourseIdAndStatusIn(courseId, ACTIVE_STATUSES);
        return CourseDetailResponse.from(course, count);
    }

    /**
     * 강의 상태를 변경, 강의 개설자만 호출할 가능
     *
     * 실제 상태 전이 유효성 검증은 {@link Course#transitionTo(CourseStatus)}에서 수행
     * Service는 소유권 확인만 담당
     *
     * @param courseId  상태를 변경할 강의 ID
     * @param userId    요청자 ID ({@code X-User-Id} 헤더에서 전달됨)
     * @param newStatus 변경하고자 하는 목표 상태
     * @throws com.samintech.liveklass.common.BusinessException 요청자가 해당 강의의 개설자가 아닌 경우
     */
    @Transactional
    public CourseResponse updateStatus(Long courseId, Long userId, CourseStatus newStatus) {
        Course course = courseRepository.findByIdWithCreator(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));


        if (!course.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_CREATOR);
        }

        course.transitionTo(newStatus);

        int count = enrollmentRepository.countByCourseIdAndStatusIn(courseId, ACTIVE_STATUSES);
        return CourseResponse.from(course, count);
    }
}