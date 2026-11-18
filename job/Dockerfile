FROM eclipse-temurin:25-jre
# Fail the build on a JDK older than 25.0.5+8 (e.g. a stale cached base image): its trust store still has the
# roots that signed the compromised eMudhra intermediate CAs. https://adoptium.net/news/2026/10/temurin-october-2026-emudhra/
# The listing is checked to be non-empty, so a keytool that fails does not pass the check.
RUN keytool -list -v -cacerts -storepass changeit > /tmp/cacerts.txt \
    && grep -q "^Owner: " /tmp/cacerts.txt \
    && ! grep -E "^Owner: .*CN=(emSign ECC Root CA - G3|AAA Certificate Services)\b" /tmp/cacerts.txt \
    && rm /tmp/cacerts.txt

WORKDIR /app
COPY ./target/*.jar app.jar

EXPOSE 8080
# -XX:-UseBASE64Intrinsics: run java.util.Base64 decoding as plain Java instead of the JIT intrinsic
# (reported CVE-2026-114938 in the AVX-512 decoder) until a fixed JDK 25 build is available
ENTRYPOINT ["java", "-XX:-UseBASE64Intrinsics", "-jar", "app.jar"]
