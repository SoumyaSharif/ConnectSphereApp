FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -q 2>/dev/null || true
COPY src/ src/
RUN mvn package -DskipTests -q 2>/dev/null || mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache maven 2>/dev/null; true
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
