# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
COPY pom.xml /app/pom.xml
WORKDIR /app
RUN mvn dependency:go-offline -B
COPY src /app/src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
