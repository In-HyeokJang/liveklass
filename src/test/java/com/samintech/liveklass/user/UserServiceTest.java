package com.samintech.liveklass.user;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_success() {
        // given
        UserCreateRequest request = new UserCreateRequest("newStudent", UserRole.CLASSMATE);
        User savedUser = User.builder()
                .id(100L)
                .username("newStudent")
                .role(UserRole.CLASSMATE)
                .build();

        given(userRepository.existsByUsername("newStudent")).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponse response = userService.createUser(request);

        // then
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.username()).isEqualTo("newStudent");
        assertThat(response.role()).isEqualTo(UserRole.CLASSMATE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이미 존재하는 username으로 생성 시도 시 예외 발생")
    void createUser_duplicateUsername_shouldThrow() {
        // given
        UserCreateRequest request = new UserCreateRequest("duplicateUser", UserRole.CREATOR);
        given(userRepository.existsByUsername("duplicateUser")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.USER_ALREADY_EXISTS.getMessage());
    }
}
