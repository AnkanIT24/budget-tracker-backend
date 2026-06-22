FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", \
  "-Djava.net.preferIPv4Stack=true", \
  "-Dspring.profiles.active=prod", \
  "-Dserver.address=0.0.0.0", \
  "-Dserver.port=10000", \
  "-jar", "app.jar"]