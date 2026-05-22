# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 의존성 레이어 캐시: 소스 변경 시 재다운로드 방지
COPY gradlew .
COPY gradle gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet || true

# 소스 빌드 (테스트 제외)
COPY src src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Run (JRE만 포함해 이미지 경량화)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
