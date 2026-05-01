FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar producer-sobel.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "producer-sobel.jar"]