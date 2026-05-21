FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN apk add --no-cache wget

WORKDIR /app
COPY target/code-review-bot-*.jar app.jar

EXPOSE 8099

USER appuser

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/logs/heapdump.hprof", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
