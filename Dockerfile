FROM gradle:8.12.1-jdk21-alpine AS build
WORKDIR /home/gradle/project
COPY . .
RUN chmod +x gradlew && ./gradlew :web:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /home/gradle/project/web/build/libs/*.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
