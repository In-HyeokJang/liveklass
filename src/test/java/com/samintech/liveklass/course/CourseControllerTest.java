package com.samintech.liveklass.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import com.samintech.liveklass.enrollment.EnrollmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CourseService courseService;
    @MockitoBean EnrollmentService enrollmentService;

    @Test
    @DisplayName("강의 생성 - 201 Created 반환")
    void createCourse_shouldReturn201() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(
                "Spring Boot 강의", "설명", 50000, 30,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(40));

        CourseResponse response = new CourseResponse(
                1L, "Spring Boot 강의", "설명", 50000, 30, 0,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(40), CourseStatus.DRAFT);

        given(courseService.createCourse(anyLong(), any())).willReturn(response);

        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value("Spring Boot 강의"));
    }

    @Test
    @DisplayName("강의 생성 - 제목 누락 시 400 반환")
    void createCourse_shouldReturn400WhenTitleBlank() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(
                "", "설명", 0, 10,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(30));

        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("강의 목록 조회 - 200 반환")
    void getCourses_shouldReturn200() throws Exception {
        CourseResponse response = new CourseResponse(
                1L, "강의", "설명", 10000, 20, 5,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(30), CourseStatus.OPEN);

        given(courseService.getCourses(any(), any(), any(), any(), any(), any(), any())).willReturn(List.of(response));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].enrolledCount").value(5));
    }

    @Test
    @DisplayName("강의 상세 조회 - 존재하지 않는 강의 404 반환")
    void getCourse_shouldReturn404WhenNotFound() throws Exception {
        given(courseService.getCourse(999L))
                .willThrow(new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("상태 변경 - 성공")
    void updateStatus_shouldReturn200() throws Exception {
        CourseStatusUpdateRequest request = new CourseStatusUpdateRequest(CourseStatus.OPEN);
        CourseResponse response = new CourseResponse(
                1L, "강의", "설명", 0, 10, 0,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(30), CourseStatus.OPEN);

        given(courseService.updateStatus(anyLong(), anyLong(), any())).willReturn(response);

        mockMvc.perform(patch("/api/courses/1/status")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @DisplayName("X-User-Id 헤더 누락 시 400 반환")
    void createCourse_shouldReturn400WhenHeaderMissing() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(
                "강의", "설명", 0, 10,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(30));

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("강의 생성 - 요청 본문(Body)이 완전히 비어있을 때 400 반환")
    void createCourse_shouldReturn400WhenBodyEmpty() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청 본문(Request Body)이 누락되었거나 JSON 형식이 잘못되었습니다"));
    }
}
