# 백엔드 Dockerfile
FROM gradle:8-jdk17 AS builder

WORKDIR /app

# Gradle 캐시를 활용하기 위해 build.gradle과 settings.gradle 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN gradle bootJar --no-daemon

# 런타임 스테이지
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# .env 파일 복사 (선택적, 없어도 됨)
# Docker Compose에서 환경 변수로 전달되므로 주석 처리
# COPY .env .env

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

