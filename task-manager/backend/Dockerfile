# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom first and download dependencies. Docker caches this layer, so
# dependencies are only re-downloaded when pom.xml changes.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Now copy the source and build the executable jar (tests run in CI, not here).
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ---------- Stage 2: run the jar on a small JRE image ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user (good practice).
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
