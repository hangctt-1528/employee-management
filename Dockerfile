# syntax=docker/dockerfile:1

# ---- Stage 1: build the jar with Maven + JDK 25 ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# The cache mount keeps ~/.m2 between builds, so dependencies are downloaded only once.
# No -q flag: Maven prints each download, so you can see the build is progressing.
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

# ---- Stage 2: run on a Java 25 JRE ----
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
