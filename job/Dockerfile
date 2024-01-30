FROM openjdk:18-jdk-slim
ADD ./target/*.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
