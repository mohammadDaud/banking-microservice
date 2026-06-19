FROM eclipse-temurin:21-jdk

WORKDIR /config-server

COPY target/*.jar config-server-0.0.1-SNAPSHOT.jar

EXPOSE 8888

ENTRYPOINT ["java","-jar","config-server-0.0.1-SNAPSHOT.jar"]