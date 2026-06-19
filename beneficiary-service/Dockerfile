FROM eclipse-temurin:21-jdk

WORKDIR /beneficiary-service

COPY target/*.jar beneficiary-service-0.0.1-SNAPSHOT.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","beneficiary-service-0.0.1-SNAPSHOT.jar"]