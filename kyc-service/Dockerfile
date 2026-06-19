FROM eclipse-temurin:21-jdk

WORKDIR /kyc-service

COPY target/*.jar kyc-service-0.0.1-SNAPSHOT.jar

EXPOSE 8092

ENTRYPOINT ["java","-jar","kyc-service-0.0.1-SNAPSHOT.jar"]