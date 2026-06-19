FROM eclipse-temurin:21-jdk

WORKDIR /audit-log-service

COPY target/*.jar audit-log-service-0.0.1-SNAPSHOT.jar

EXPOSE 8091

ENTRYPOINT ["java","-jar","audit-log-service-0.0.1-SNAPSHOT.jar"]