package com.samintech.liveklass.user;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자(User) 도메인 엔티티.
 *
 * 이 시스템의 인증은 JWT 없이 HTTP 요청 헤더 {@code X-User-Id}로 사용자를 식별
 * 실제 서비스라면 JWT나 OAuth를 도입해야 하지만, 과제 요구사항에 따라 단순화된 구조
 *
 * 역할({@link UserRole})에 따라 접근 가능한 기능이 달라진다.
 * {@code CREATOR}는 강의를 관리하고, {@code CLASSMATE}는 수강을 신청
 */

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
}