# Dockerfile used in compose to build and run project.
FROM openjdk:21-jdk-slim as builder

RUN apt update && apt install -y maven && apt clean

ADD . /app
WORKDIR /app
RUN mvn clean package

FROM openjdk:21-jdk-slim

RUN apt update && apt install -y git && apt clean

COPY --from=builder /app/api/target/*.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
