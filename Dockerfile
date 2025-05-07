FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY ./gradle ./gradle
COPY ./build.gradle.kts ./build.gradle.kts
COPY ./gradle.properties ./gradle.properties
COPY ./gradlew ./gradlew
COPY ./settings.gradle.kts ./settings.gradle.kts

RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY ./launchers ./launchers
COPY ./resources ./resources

RUN ./gradlew launchers:connector:build --exclude-task javadoc -x checkstyleMain -x checkstyleTest

FROM eclipse-temurin:17-ubi9-minimal

WORKDIR /app

COPY --from=build /app/launchers/connector/build/libs/connector.jar connector.jar

RUN adduser --system --no-create-home appuser

USER appuser

HEALTHCHECK --interval=120s --timeout=20s --retries=3 CMD curl -f http://localhost:${WEB_HTTP_PORT}/api/check/health || exit 1

CMD ["java", "-jar", "/app/connector.jar"]
