FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle build.gradle ./
COPY gradle gradle
COPY mog-contract mog-contract
COPY mog-core mog-core
COPY mog-api mog-api

RUN chmod +x gradlew
RUN ./gradlew --no-daemon :mog-api:bootJar

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV MOG_HOME=/opt/mog
ENV MOG_ETC=/opt/mog/etc
ENV JAVA_OPTS=""

RUN mkdir -p /opt/mog/etc /opt/mog/var

COPY --from=build /workspace/mog-api/build/libs/*.jar /app/mog-api.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/mog-api.jar"]
