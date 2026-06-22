FROM eclipse-temurin:21-jdk

WORKDIR /rule-engine-service

COPY target/*.jar rule-engine-service-0.0.1-SNAPSHOT.jar

EXPOSE 8093

ENTRYPOINT ["java","-jar","rule-engine-service-0.0.1-SNAPSHOT.jar"]