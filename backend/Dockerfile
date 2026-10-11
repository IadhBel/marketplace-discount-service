# Expects the executable jar built beforehand by `./gradlew :backend:bootJar` (done by the CI build job).

FROM eclipse-temurin:25-jre AS extract
WORKDIR /builder
COPY build/libs/*.jar application.jar
# Split the jar into layers so that dependency layers are cached between builds
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 spring
USER spring
WORKDIR /application
COPY --from=extract /builder/extracted/dependencies/ ./
COPY --from=extract /builder/extracted/spring-boot-loader/ ./
COPY --from=extract /builder/extracted/snapshot-dependencies/ ./
COPY --from=extract /builder/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
