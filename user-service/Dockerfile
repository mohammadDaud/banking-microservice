FROM eclipse-temurin:21-jdk

WORKDIR /user-service

COPY target/*.jar user-service-0.0.1-SNAPSHOT.jar

EXPOSE 8085

ENTRYPOINT ["java","-jar","user-service-0.0.1-SNAPSHOT.jar"]