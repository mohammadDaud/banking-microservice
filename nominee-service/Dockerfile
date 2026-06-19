FROM eclipse-temurin:21-jdk

WORKDIR /nominee-service

COPY target/*.jar nominee-service-0.0.1-SNAPSHOT.jar

EXPOSE 8087

ENTRYPOINT ["java","-jar","nominee-service-0.0.1-SNAPSHOT.jar"]