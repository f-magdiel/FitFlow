# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY notif-api/pom.xml notif-api/pom.xml
COPY notif-service/pom.xml notif-service/pom.xml
COPY notif-dao/pom.xml notif-dao/pom.xml
COPY notif-server/pom.xml notif-server/pom.xml

RUN mvn dependency:go-offline

COPY . .

RUN mvn -pl notif-server -am clean package -DskipTests


# ---------- Runtime ----------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Environment variables for runtime (available inside container)
ENV NOTIF_DB_NAME=notifdb
ENV NOTIF_DB_USER=notif_user
ENV NOTIF_DB_PASSWORD=notif_pass

COPY --from=build /app/notif-server/target/*.jar app.jar

EXPOSE 8002

ENTRYPOINT ["java", "-jar", "app.jar"]