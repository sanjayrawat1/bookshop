FROM eclipse-temurin:19 AS builder
WORKDIR workspace
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} catalog-service.jar
RUN java -Djarmode=layertools -jar catalog-service.jar extract

FROM eclipse-temurin:19
# You should be aware that containers run using the root user by default, potentially letting them get root access to the Docker host.
# You can mitigate the risk by creating a non-privileged user and using it to run the entry-point process defined in the Dockerfile,
# following the principle of least privilege. You can improve it by adding new steps to create a new non-root user that will run the application.
# creates a "spring" user
RUN useradd spring
# configures "spring" as the current user
USER spring
WORKDIR workspace
COPY --from=builder workspace/dependencies/ ./
COPY --from=builder workspace/spring-boot-loader/ ./
COPY --from=builder workspace/snapshot-dependencies/ ./
COPY --from=builder workspace/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
