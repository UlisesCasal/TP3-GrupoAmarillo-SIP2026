FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre

RUN groupadd -r producergroup && useradd -r -g producergroup produceruser
RUN mkdir -p /app/result && chown produceruser:producergroup /app/result
WORKDIR /app
USER produceruser

COPY --chown=produceruser:producergroup --from=build /app/target/*.jar producer-sobel.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "producer-sobel.jar"]