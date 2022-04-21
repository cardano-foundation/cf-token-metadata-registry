FROM openjdk:17-jdk-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /cf-metafides-api/

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /cf-metafides-api/app.jar

COPY run_backend.sh /cf-metafides-api/run_backend.sh

ENTRYPOINT ["/bin/sh", "/cf-metafides-api/run_backend.sh"]
