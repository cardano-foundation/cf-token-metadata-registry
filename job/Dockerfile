# Pinned to the exact Temurin build, not the floating 25-jre tag, so an image is known to carry the October 2026
# trust store: 25.0.5+8 removes the eMudhra roots whose intermediates were compromised (Adoptium, 2026-10-27).
# A floating tag gives whatever was cached or current when the image was built.
FROM eclipse-temurin:25.0.5_8-jre

WORKDIR /app
COPY ./target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
