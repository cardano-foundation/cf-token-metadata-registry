FROM openjdk:18-jdk-slim as runner

WORKDIR /cf-metadata-api/
RUN mkdir /cardano-token-registry

ARG GH_PACKAGES_USER_NAME
ARG GH_PACKAGES_ACCESS_TOKEN
ARG GH_PACKAGES_ACCESS_TOKEN_TEST

ENV GH_PACKAGES_USER_NAME=$GH_PACKAGES_USER_NAME
ENV GH_PACKAGES_ACCESS_TOKEN=$GH_PACKAGES_ACCESS_TOKEN
ENV GH_PACKAGES_ACCESS_TOKEN_TEST=$GH_PACKAGES_ACCESS_TOKEN_TEST

RUN curl -X POST -H "Accept: application/vnd.github+json" -H "Authorization: Bearer ${GH_PACKAGES_ACCESS_TOKEN}" https://api.github.com/repos/cardano-foundation/cardano-token-registry/forks
RUN addgroup --system spring && adduser --system --ingroup spring spring
RUN chown spring:spring /cf-metadata-api
RUN chown spring:spring /cardano-token-registry

USER spring:spring


ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /cf-metadata-api/app.jar

COPY run_backend.sh /cf-metadata-api/run_backend.sh
