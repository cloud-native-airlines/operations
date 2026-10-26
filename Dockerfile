FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

# Wrapper + build config first for layer caching.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle VERSION ./
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/build/libs/*.jar /app/operations.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/operations.jar"]
