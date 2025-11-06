FROM eclipse-temurin:21-jdk-alpine

RUN apt update && apt install -y git && apt clean

ADD ./target/*.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
