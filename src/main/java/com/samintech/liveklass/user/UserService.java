package com.samintech.liveklass.user;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.username())
                .role(request.role())
                .build();
        return UserResponse.from(userRepository.save(user));
    }
}
