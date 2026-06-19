FROM eclipse-temurin:21-jdk

WORKDIR /discovery-service

COPY target/*.jar discovery-service-0.0.1-SNAPSHOT.jar

EXPOSE 8761

ENTRYPOINT ["java","-jar","discovery-service-0.0.1-SNAPSHOT.jar"]