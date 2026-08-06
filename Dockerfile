FROM eclipse-temurin:21-jre-jammy

RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --home-dir /app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

COPY --chown=10001:10001 build/libs/pragmatic-jvm-demo.jar /app/app.jar

USER 10001:10001

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
