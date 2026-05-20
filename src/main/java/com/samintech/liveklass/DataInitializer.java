package com.samintech.liveklass;

import com.samintech.liveklass.user.User;
import com.samintech.liveklass.user.UserRepository;
import com.samintech.liveklass.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.saveAll(List.of(
                    User.builder().username("creator1").role(UserRole.CREATOR).build(),
                    User.builder().username("creator2").role(UserRole.CREATOR).build(),
                    User.builder().username("student1").role(UserRole.CLASSMATE).build(),
                    User.builder().username("student2").role(UserRole.CLASSMATE).build(),
                    User.builder().username("student3").role(UserRole.CLASSMATE).build()
            ));
            log.info("시드 데이터 초기화 완료: creator(id=1,2), student(id=3,4,5)");
        }
    }
}
