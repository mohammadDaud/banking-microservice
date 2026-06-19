FROM eclipse-temurin:21-jdk

WORKDIR /transaction-service

COPY target/*.jar transaction-service-0.0.1-SNAPSHOT.jar

EXPOSE 8089

ENTRYPOINT ["java","-jar","transaction-service-0.0.1-SNAPSHOT.jar"]