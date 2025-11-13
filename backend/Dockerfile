# 배포용 Dockerfile (GitHub Actions에서 빌드된 JAR 사용)
FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=build/libs/demo-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} /app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/app.jar"]

