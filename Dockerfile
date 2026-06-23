FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/budget-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-Dspring.profiles.active=prod", "-Dserver.address=0.0.0.0", "-Dserver.port=10000", "-jar", "app.jar"]