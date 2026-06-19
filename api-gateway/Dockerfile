FROM eclipse-temurin:21-jdk

WORKDIR /api-gateway

COPY target/*.jar api-gateway-0.0.1-SNAPSHOT.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","api-gateway-0.0.1-SNAPSHOT.jar"]