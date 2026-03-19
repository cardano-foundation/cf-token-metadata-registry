# Dockerfile used in compose to build and run project.
FROM eclipse-temurin:25-jdk AS builder

RUN apt-get update && apt-get install -y maven && apt-get clean

ADD . /app
WORKDIR /app
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre

COPY --from=builder /app/api/target/*.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
