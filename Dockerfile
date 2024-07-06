# Etapa de compilación
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM openjdk:17.0.1-jdk-slim
WORKDIR /app
COPY --from=build /app/target/authorization-0.0.1-SNAPSHOT.jar authorization.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "authorization.jar"]
