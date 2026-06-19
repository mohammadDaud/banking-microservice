FROM eclipse-temurin:21-jdk

WORKDIR /auth-service

COPY target/*.jar auth-service-0.0.1-SNAPSHOT.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","auth-service-0.0.1-SNAPSHOT.jar"]