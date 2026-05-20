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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

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

    public List<CourseResponse> getCourses(CourseStatus status) {
        List<Course> courses = status != null
                ? courseRepository.findByStatus(status)
                : courseRepository.findAll();

        if (courses.isEmpty()) return List.of();

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, Integer> countMap = enrollmentRepository
                .countByCourseIdsAndStatusIn(courseIds, ACTIVE_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        return courses.stream()
                .map(c -> CourseResponse.from(c, countMap.getOrDefault(c.getId(), 0)))
                .toList();
    }

    public CourseDetailResponse getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        int count = enrollmentRepository.countByCourseIdAndStatusIn(courseId, ACTIVE_STATUSES);
        return CourseDetailResponse.from(course, count);
    }

    @Transactional
    public CourseResponse updateStatus(Long courseId, Long userId, CourseStatus newStatus) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_CREATOR);
        }

        course.transitionTo(newStatus);

        int count = enrollmentRepository.countByCourseIdAndStatusIn(courseId, ACTIVE_STATUSES);
        return CourseResponse.from(course, count);
    }
}