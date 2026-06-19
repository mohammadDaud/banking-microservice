FROM eclipse-temurin:21-jdk

WORKDIR /accountnumbergen-service

COPY target/*.jar accountnumbergen-service-0.0.1-SNAPSHOT.jar

EXPOSE 8083

ENTRYPOINT ["java","-jar","accountnumbergen-service-0.0.1-SNAPSHOT.jar"]