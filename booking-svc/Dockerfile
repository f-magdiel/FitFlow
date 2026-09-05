### Etapa 1: build ###
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sh ./mvnw -B dependency:go-offline

COPY src ./src
RUN sh ./mvnw -B clean package -DskipTests

### Etapa 2: runtime ###
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S fitflow && adduser -S fitflow -G fitflow
COPY --from=build /app/target/*.jar app.jar
RUN chown fitflow:fitflow app.jar
USER fitflow

EXPOSE 8001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
