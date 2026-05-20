package com.samintech.liveklass.course;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import com.samintech.liveklass.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 강의(Course) 도메인 엔티티
 * 크리에이터가 개설하는 강의 정보를 담고 있으며, 강의 상태(DRAFT, OPEN, CLOSED) 전이를 책임집니다.
 * 비즈니스 규칙(상태 전이 검증)을 도메인 내부에 두어 응집도를 높였습니다.
 */
@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 강의 상태를 변경합니다.
     * 외부(Service)에서 직접 상태값을 변경(setter)하지 않고, 이 메서드를 통해서만 변경하도록 하여
     * 올바른 상태 전이(DRAFT -> OPEN, OPEN -> CLOSED)인지 엔티티 스스로 검증합니다.
     *
     * @param newStatus 변경하고자 하는 새로운 상태
     * @throws BusinessException 잘못된 상태 전이일 경우 예외 발생
     */
    public void transitionTo(CourseStatus newStatus) {
        if (!isValidTransition(this.status, newStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = newStatus;
    }

    /**
     * 유효한 상태 전이인지 확인합니다.
     * - DRAFT (초안) -> OPEN (모집 중) : 가능
     * - OPEN (모집 중) -> CLOSED (모집 마감) : 가능
     * - 그 외 (예: DRAFT -> CLOSED, CLOSED -> OPEN 등) : 불가능
     */
    private boolean isValidTransition(CourseStatus from, CourseStatus to) {
        return (from == CourseStatus.DRAFT && to == CourseStatus.OPEN)
                || (from == CourseStatus.OPEN && to == CourseStatus.CLOSED);
    }
}