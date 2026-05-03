FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre

RUN groupadd -r joinergroup && useradd -r -g joinergroup joineruser
RUN mkdir -p /app/result && chown joineruser:joinergroup /app/result
WORKDIR /app
USER joineruser

COPY --chown=joineruser:joinergroup --from=build /app/target/*.jar joiner-sobel.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "joiner-sobel.jar"]