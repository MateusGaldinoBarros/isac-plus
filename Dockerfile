FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Primeiro so o que define as dependencias: enquanto o pom.xml nao mudar, essa camada vem do cache
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]
