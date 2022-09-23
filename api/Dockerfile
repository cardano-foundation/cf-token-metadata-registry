FROM openjdk:18-jdk-slim as runner

WORKDIR /cf-metadata-api/

ARG GH_PACKAGES_USER_NAME
ARG GH_PACKAGES_ACCESS_TOKEN
ARG GH_PACKAGES_ACCESS_TOKEN_TEST

ENV GH_PACKAGES_USER_NAME=$GH_PACKAGES_USER_NAME
ENV GH_PACKAGES_ACCESS_TOKEN=$GH_PACKAGES_ACCESS_TOKEN
ENV GH_PACKAGES_ACCESS_TOKEN_TEST=$GH_PACKAGES_ACCESS_TOKEN_TEST

RUN addgroup --system spring && adduser --system --ingroup spring spring
RUN chown spring:spring /cf-metadata-api
USER spring:spring

RUN mkdir /token-registry-repository

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /cf-metadata-api/app.jar

COPY run_backend.sh /cf-metadata-api/run_backend.sh
