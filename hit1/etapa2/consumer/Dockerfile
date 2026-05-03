FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r consumergroup && useradd -r -g consumergroup consumeruser
COPY --chown=consumeruser:consumergroup --from=build /app/target/*.jar consumer-sobel.jar

USER consumeruser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "consumer-sobel.jar"]