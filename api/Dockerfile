FROM openjdk:18-jdk-slim as runner

WORKDIR /cf-metadata-api/
RUN mkdir /cardano-token-registry

ARG GH_PACKAGES_USER_NAME
ARG GH_PACKAGES_ACCESS_TOKEN

ENV GH_PACKAGES_USER_NAME=$GH_PACKAGES_USER_NAME
ENV GH_PACKAGES_ACCESS_TOKEN=$GH_PACKAGES_ACCESS_TOKEN

RUN addgroup --system spring && adduser --system --ingroup spring spring
RUN chown spring:spring /cf-metadata-api
RUN chown spring:spring /cardano-token-registry

USER spring:spring


ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /cf-metadata-api/app.jar

COPY run_backend.sh /cf-metadata-api/run_backend.sh
