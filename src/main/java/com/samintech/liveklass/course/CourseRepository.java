package com.samintech.liveklass.course;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(CourseStatus status);

    /**
     * 정원 초과 동시 신청을 막기 위한 비관적 락(Pessimistic Lock) 메서드입니다.
     * 데이터베이스 레벨에서 `SELECT ... FOR UPDATE` 쿼리가 실행되며,
     * 트랜잭션이 끝날 때까지 다른 트랜잭션은 이 레코드를 수정하거나 같은 락을 얻을 수 없습니다.
     * 이로 인해 동시에 여러 명이 마지막 자리를 신청하더라도 순차적으로 처리되어 정원 초과를 방지합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithLock(@Param("id") Long id);
}