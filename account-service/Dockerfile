FROM eclipse-temurin:21-jdk

WORKDIR /account-service

COPY target/*.jar account-service-0.0.1-SNAPSHOT.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","account-service-0.0.1-SNAPSHOT.jar"]