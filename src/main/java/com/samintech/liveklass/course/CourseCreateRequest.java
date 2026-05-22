package com.samintech.liveklass.course;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 강의 생성 요청 DTO
 *
 * <p>각 필드에 붙은 {@code @NotBlank}, {@code @Min} 등의 애노테이션은 Bean Validation 규칙
 * Controller에서 {@code @Validated}를 사용할 때 자동으로 검증되며,
 * 실패 시 {@link com.samintech.liveklass.common.GlobalExceptionHandler}가 400 응답을 반환
 *
 * @param title       강의 제목 (필수, 공백 불가)
 * @param description 강의 설명 (선택)
 * @param price       수강료 (0 이상, 무료 강의는 0)
 * @param capacity    정원 (1명 이상)
 * @param startDate   강의 시작일 (필수)
 * @param endDate     강의 종료일 (필수, startDate 이후여야 함)
 */

public record CourseCreateRequest(
        @NotBlank(message = "제목은 필수입니다") String title,
        String description,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다") int price,
        @Min(value = 1, message = "정원은 1명 이상이어야 합니다") int capacity,
        @NotNull(message = "시작일은 필수입니다")
        @FutureOrPresent(message = "시작일은 오늘 이후여야 합니다") LocalDate startDate,
        @NotNull(message = "종료일은 필수입니다")
        @FutureOrPresent(message = "종료일은 오늘 이후여야 합니다") LocalDate endDate
) {}
