FROM openjdk:17-jdk-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /cf-metadata-api/

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /cf-metadata-api/app.jar

COPY run_backend.sh /cf-metadata-api/run_backend.sh

ENTRYPOINT ["/bin/sh", "/cf-metadata-api/run_backend.sh"]
