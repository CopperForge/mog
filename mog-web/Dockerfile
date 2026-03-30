FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle build.gradle ./
COPY gradle gradle
COPY mog-contract mog-contract
COPY mog-web mog-web

RUN chmod +x gradlew
RUN ./gradlew --no-daemon :mog-web:bootJar

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=build /workspace/mog-web/build/libs/*.jar /app/mog-web.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/mog-web.jar"]
