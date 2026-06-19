FROM eclipse-temurin:21-jdk

WORKDIR /notification-service

COPY target/*.jar notification-service-0.0.1-SNAPSHOT.jar

EXPOSE 8082

ENTRYPOINT ["java","-jar","notification-service-0.0.1-SNAPSHOT.jar"]