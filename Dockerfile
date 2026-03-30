FROM amazoncorretto:21
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
COPY docs/api /app/api-docs

RUN mkdir -p /app/logs && chmod -R 755 /app/logs
VOLUME ["/app/logs"]

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Duser.timezone=GMT+9 -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
